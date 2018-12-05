package ch.hsr.ifs.sconsolidator.core;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;

public final class PlatformSpecifics {
  public static final String NEW_LINE = System.getProperty("line.separator");
  public static final Pattern CPP_RE = Pattern.compile("(.+?(?:\\.(?:cpp|c|cc|C|cxx|h|hxx|hpp|ipp)))[:(]?([\\d]+)[)]?");
  public static final Pattern FORT_RE = Pattern.compile("(?i)(.+?(?:\\.(?:f95|f90|f|f77|for))):([\\d]+)(?:.([\\d]+))?:");
  private static final Pattern OBJ_FILE_RE = Pattern.compile("([^\\s]+)(\\.(o|os|obj))$");
  private static final String SCONS_EXECUTABLE_NAME = isWindows() ? "scons.bat" : "scons";

  private PlatformSpecifics() {}

  public static boolean isObjectFile(String file) {
    return OBJ_FILE_RE.matcher(file).matches();
  }

  public static String getSConsCommandName() {
    return SCONS_EXECUTABLE_NAME;
  }

  private static boolean isWindows() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows");
  }

  public static File findExecOnSystemPath(String executableName) {
    String systemPath = System.getenv("PATH");
    String[] pathDirs = systemPath.split(File.pathSeparator);

    for (String pathDir : pathDirs) {
      File file = new File(pathDir, executableName);

      if (file.isFile())
        return file;
    }
    return null;
  }

  public static File findSConsExecOnSystemPath() {
    return findExecOnSystemPath(SCONS_EXECUTABLE_NAME);
  }

  public static int getNumberOfAvalaibleProcessors() {
    return Runtime.getRuntime().availableProcessors();
  }

  public static Map<String, String> getSystemEnv() {
    return System.getenv();
  }

  static String expandEnvVariables(String toExpand, boolean keepInvalid) {
    Pattern envVarRe = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
    Matcher matcher = envVarRe.matcher(toExpand);
    Map<String, String> env = getSystemEnv();

    while (matcher.find()) {
      String envVal = env.get(matcher.group(1).toUpperCase());

      if (envVal == null) {
        if (keepInvalid) {
          continue;
        }
        envVal = "";
      } else {
        envVal = envVal.replace("\\", "\\\\");
      }

      Pattern subExp = Pattern.compile(Pattern.quote(matcher.group(0)));
      toExpand = subExp.matcher(toExpand).replaceAll(envVal);
    }
    return toExpand;
  }

  public static String expandEnvVariables(String toExpand) {
    return expandEnvVariables(toExpand, false);
  }

  public static String expandEnvAndBuildVariables(String toExpand, IProject project) {
    toExpand = expandEnvVariables(toExpand, true);

    ICProjectDescription projDescription = CoreModel.getDefault().getProjectDescription(project);
    if (projDescription != null) {
      ICConfigurationDescription cfg = projDescription.getActiveConfiguration();
      try {
        toExpand = CCorePlugin.getDefault().getCdtVariableManager().resolveValue(toExpand, "", null, cfg);
      } catch (CdtVariableException e) {
        e.printStackTrace();
      }
    }
    return toExpand;
  }
}
