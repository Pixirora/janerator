package dev.pixirora.janerator;

import java.util.Arrays;
import java.util.List;

import org.quiltmc.config.api.WrappedConfig;
import org.quiltmc.loader.api.config.QuiltConfig;

public class JaneratorConfig extends WrappedConfig {
    public static final JaneratorConfig INSTANCE = QuiltConfig.create("janerator", "preset", JaneratorConfig.class);

    public final OverrideSelection override_selection = new OverrideSelection();

    public static String getOverrideFunction() {
        return (String) JaneratorConfig.INSTANCE.getValue(List.of("override_selection", "override_function")).value();
    }

    public static List<String> getLeadUpFunctions() {
        String leadUpFunctions = (String) JaneratorConfig.INSTANCE.getValue(List.of("override_selection", "variables_in_override_function")).value();
        return Arrays.asList(leadUpFunctions.split("\n"));
    }

    public static class OverrideSelection implements Section {
        public final String variables_in_override_function = String.join(
            "\n",
            List.of(
                "dist_squared(x,z)=x^2+z^2",
                "angle(x,z,dist_squared)=log2(dist_squared)"
            )
        );
        public final String override_function = "f(x,z,dist_squared,angle)=(z-x*tan(angle))*sgn(tan(angle)*csc(angle))";
    }
}
