package net.austizz.lostcitiesroadfixes.compat;

import net.neoforged.fml.ModList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class LostCitiesCompatibility {
    public static final String EXPECTED_VERSION = "1.21-8.3.10";
    public static final String EXPECTED_SHA256 =
            "26db73013028ad724af030aa30cbd8dd62da8b951645b8d02f066c8af083a52f";

    private static final String HIGHWAY = "mcjty/lostcities/worldgen/lost/Highway";
    private static final String HIGHWAYS = "mcjty/lostcities/worldgen/gen/Highways";
    private static final String BUILDING_INFO = "mcjty/lostcities/worldgen/lost/BuildingInfo";
    private static final String TERRAIN_FEATURE = "mcjty/lostcities/worldgen/LostCityTerrainFeature";

    private static final String HIGHWAY_LEVEL_DESCRIPTOR =
            "(Lmcjty/lostcities/varia/ChunkCoord;Lmcjty/lostcities/worldgen/IDimensionInfo;"
                    + "Lmcjty/lostcities/config/LostCityProfile;)I";
    private static final String GENERATE_HIGHWAYS_DESCRIPTOR =
            "(Lmcjty/lostcities/worldgen/LostCityTerrainFeature;"
                    + "Lmcjty/lostcities/worldgen/lost/BuildingInfo;)V";
    private static final String HAS_HIGHWAY_DESCRIPTOR =
            "(Lmcjty/lostcities/varia/ChunkCoord;Lmcjty/lostcities/worldgen/IDimensionInfo;"
                    + "Lmcjty/lostcities/config/LostCityProfile;)Z";
    private static final String FIX_AFTER_EXPLOSION_DESCRIPTOR =
            "(Lmcjty/lostcities/worldgen/lost/BuildingInfo;)V";

    private LostCitiesCompatibility() {
    }

    public static CompatibilityReport inspect() {
        List<String> verified = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String version = "unknown";
        String sha256 = "unknown";

        try {
            Path jarPath = findLostCitiesJar();
            version = implementationVersion(jarPath);
            sha256 = sha256(jarPath);

            if (!EXPECTED_VERSION.equals(version)) {
                errors.add("expected version " + EXPECTED_VERSION + " but found " + version);
            }
            if (!EXPECTED_SHA256.equals(sha256)) {
                errors.add("expected SHA-256 " + EXPECTED_SHA256 + " but found " + sha256);
            }

            verifyMethod(jarPath, verified, HIGHWAY, "getXHighwayLevel", HIGHWAY_LEVEL_DESCRIPTOR, true);
            verifyMethod(jarPath, verified, HIGHWAY, "getZHighwayLevel", HIGHWAY_LEVEL_DESCRIPTOR, true);
            verifyMethod(jarPath, verified, HIGHWAYS, "generateHighways", GENERATE_HIGHWAYS_DESCRIPTOR, true);
            verifyMethod(jarPath, verified, BUILDING_INFO, "hasHighway", HAS_HIGHWAY_DESCRIPTOR, true);
            verifyMethod(jarPath, verified, TERRAIN_FEATURE, "fixAfterExplosion",
                    FIX_AFTER_EXPLOSION_DESCRIPTOR, false);
        } catch (IOException | URISyntaxException | NoSuchAlgorithmException exception) {
            errors.add(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }

        return new CompatibilityReport(errors.isEmpty(), version, sha256, verified, errors);
    }

    public static void requireCompatible() {
        CompatibilityReport report = inspect();
        if (!report.compatible()) {
            throw new IllegalStateException(report.diagnostic());
        }
    }

    private static void verifyMethod(Path jarPath, List<String> verified, String owner, String name,
                                     String descriptor, boolean expectedStatic) throws IOException {
        String classEntry = owner + ".class";
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(classEntry);
            if (entry == null) {
                throw new IOException("missing class " + owner.replace('/', '.'));
            }
            boolean[] matched = {false};
            try (var input = jar.getInputStream(entry)) {
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String candidateName,
                                                     String candidateDescriptor, String signature,
                                                     String[] exceptions) {
                        if (name.equals(candidateName) && descriptor.equals(candidateDescriptor)
                                && expectedStatic == ((access & Opcodes.ACC_STATIC) != 0)) {
                            matched[0] = true;
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            if (!matched[0]) {
                throw new IOException("missing method " + owner.replace('/', '.') + "#" + name + descriptor);
            }
        }
        verified.add(owner.replace('/', '.') + "#" + name);
    }

    private static Path findLostCitiesJar() throws URISyntaxException, IOException {
        URL resource = LostCitiesCompatibility.class.getClassLoader().getResource(HIGHWAY + ".class");
        if (resource != null && "jar".equals(resource.getProtocol())) {
            URL jarUrl = ((JarURLConnection) resource.openConnection()).getJarFileURL();
            Path path = Path.of(jarUrl.toURI());
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        if (resource != null && "union".equals(resource.getProtocol())) {
            String encodedPath = resource.toExternalForm().substring("union:".length());
            int marker = encodedPath.indexOf("%23");
            if (marker < 0) {
                marker = encodedPath.indexOf("!/");
            }
            if (marker > 0) {
                Path path = Path.of(URI.create("file:" + encodedPath.substring(0, marker)));
                if (Files.isRegularFile(path)) {
                    return path;
                }
            }
        }

        ModList modList = ModList.get();
        if (modList != null && modList.getModFileById("lostcities") != null) {
            Path path = modList.getModFileById("lostcities").getFile().getFilePath();
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        throw new IOException("could not locate the loaded Lost Cities JAR");
    }

    private static String implementationVersion(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            if (jar.getManifest() == null) {
                return "unknown";
            }
            Attributes attributes = jar.getManifest().getMainAttributes();
            return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
    }

    private static String sha256(Path jarPath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(jarPath)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
