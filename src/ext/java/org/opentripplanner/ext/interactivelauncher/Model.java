package org.opentripplanner.ext.interactivelauncher;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Transient;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

public class Model implements Serializable {
  private static final File MODEL_FILE = new File("interactive_otp_main.json");

  private final Map<String, Boolean> debugLogging = new HashMap<>();

  private String rootDirectory = null;
  private String dataSource = null;
  private boolean buildStreet = false;
  private boolean buildTransit = true;
  private boolean saveGraph = false;
  private boolean serveGraph = true;


  public Model() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger log : context.getLoggerList()) {
      if(log.getName().matches("org\\.opentripplanner\\..*"))  {
        debugLogging.put(log.getName(), Boolean.FALSE);
      }
    }
  }

  @SuppressWarnings("AccessOfSystemProperties")
  public String getRootDirectory() {
    return rootDirectory == null
        ? System.getProperty("user.dir")
        : rootDirectory;
  }

  public void setRootDirectory(String rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public String getDataSource() {
    return dataSource;
  }

  public void setDataSource(String dataSource) {
    this.dataSource = dataSource;
  }

  @Transient
  String getDataSourceDirectory() {
    if(dataSource == null) {
      return "DATA_SOURCE_NOT_SET";
    }
    return rootDirectory + File.separatorChar + dataSource;
  }

  @Transient
  public List<String> getDataSourceOptions() {
    List<String> dataSourceOptions = new ArrayList<>();
    File rootDir = new File(getRootDirectory());
    List<File> dirs = SearchForOtpConfig.search(rootDir);
    // Add 1 char for the path separator character
    int length = rootDir.getAbsolutePath().length() + 1;

    for (File dir : dirs) {
      dataSourceOptions.add(dir.getAbsolutePath().substring(length));
    }
    return dataSourceOptions;
  }

  public boolean isBuildStreet() {
    return buildStreet;
  }

  public void setBuildStreet(boolean buildStreet) {
    this.buildStreet = buildStreet;
  }

  public boolean isBuildTransit() {
    return buildTransit;
  }

  public void setBuildTransit(boolean buildTransit) {
    this.buildTransit = buildTransit;
  }

  private boolean buildAll() { return isBuildStreet() && isBuildTransit(); }

  private boolean buildStreetOnly() { return isBuildStreet() && !isBuildTransit(); }

  public boolean isSaveGraph() {
    return saveGraph;
  }

  public void setSaveGraph(boolean saveGraph) {
    this.saveGraph = saveGraph;
  }

  public boolean isServeGraph() {
    return serveGraph;
  }

  public void setServeGraph(boolean serveGraph) {
    this.serveGraph = serveGraph;
  }

  public Map<String, Boolean> getDebugLogging() {
    return debugLogging;
  }

  public void setDebugLogging(Map<String, Boolean> map) {
    debugLogging.putAll(map);
  }

  @Override
  public String toString() {
    return "("
        + "data-source-dir: " + getDataSourceDirectory()
        + (buildStreet ? ", buildStreet" : "")
        + (buildTransit ? ", buildTransit" : "")
        + (saveGraph ? ", saveGraph" : "")
        + (serveGraph ? ", serveGraph" : "")
        + ')';
  }

  String[] asOtpArgs() {
    List<String> args = new ArrayList<>();

    if(buildAll()) {
      args.add("--build");
    }
    else if(buildStreet) {
      args.add("--buildStreet");
    }
    else if(buildTransit) {
      args.add("--loadStreet");
    }
    else {
      args.add("--load");
    }

    if(saveGraph && (buildTransit||buildStreet)) { args.add("--save"); }
    if(serveGraph && !buildStreetOnly()) { args.add("--serve"); }

    args.add(getDataSourceDirectory());

    return args.toArray(new String[0]);
  }

  public String toCliString() {
    return String.join(" ", asOtpArgs());
  }

  public static Model load() {
    try {
      return MODEL_FILE.exists()
          ? new ObjectMapper().readValue(MODEL_FILE, Model.class)
          : new Model();
    }
    catch (IOException e) {
      System.err.println(
          "Unable to read the InteractiveOtpMain state cache. If the model changed this is "
              + "expected, and it will work next time. Cause: " + e.getMessage()
      );
      return new Model();
    }
  }


  public void save() {
    try {
      new ObjectMapper().writeValue(MODEL_FILE, this);
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
