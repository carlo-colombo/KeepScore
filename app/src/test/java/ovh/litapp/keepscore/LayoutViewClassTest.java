package ovh.litapp.keepscore;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every fully-qualified view class referenced by a layout XML must exist on the
 * classpath.
 *
 * <p>Regression net for the AndroidX migration: a layout that still references a
 * removed class (for example {@code android.support.v4.view.ViewPager}) only fails
 * at runtime when that screen is opened. This checks the layouts statically, so it
 * needs no Android runtime and is not affected by Robolectric resource quirks.
 */
public class LayoutViewClassTest {

    /** Matches a fully-qualified view tag, e.g. {@code <androidx.viewpager.widget.ViewPager}. */
    private static final Pattern VIEW_TAG =
            Pattern.compile("<([a-z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+)+)\\b");

    @Test
    public void everyLayoutViewClassExists() throws Exception {
        File res = new File("src/main/res");
        assertTrue("expected to run from the app module directory: " + res.getAbsolutePath(),
                res.isDirectory());

        List<String> failures = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(res.toPath())) {
            for (Path path : (Iterable<Path>) paths
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .filter(p -> p.getParent().getFileName().toString().startsWith("layout"))::iterator) {
                String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                Matcher matcher = VIEW_TAG.matcher(xml);
                while (matcher.find()) {
                    String className = matcher.group(1);
                    try {
                        Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        failures.add(path.getFileName() + " -> " + className);
                    }
                }
            }
        }
        assertTrue("layout(s) reference missing classes: " + failures, failures.isEmpty());
    }
}
