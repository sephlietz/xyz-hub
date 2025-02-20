/*
 * Copyright (C) 2017-2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.xyz.hub;

import com.here.xyz.hub.Service.Config;
import com.here.xyz.hub.util.ConfigDecryptor;
import com.here.xyz.hub.util.ConfigDecryptor.CryptoException;
import com.here.xyz.util.JsonConfigFile;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.CachedClock;
import org.apache.logging.log4j.core.util.NetUtils;

public class Core {

  private static final Logger logger = LogManager.getLogger();

  /**
   * The entry point to the Vert.x core API.
   */
  public static Vertx vertx;

  /**
   * A cached clock instance.
   */
  private static final CachedClock clock = CachedClock.instance();

  public static long currentTimeMillis() {
    return clock.currentTimeMillis();
  }

  /**
   * The service start time.
   */
  public static final long START_TIME = currentTimeMillis();

  /**
   * The LOG4J configuration file.
   */
  protected static final String CONSOLE_LOG_CONFIG = "log4j2-console-plain.json";

  /**
   * The Vertx worker pool size environment variable.
   */
  protected static final String VERTX_WORKER_POOL_SIZE = "VERTX_WORKER_POOL_SIZE";

  /**
   * The build time.
   */
  public static final long BUILD_TIME = getBuildTime();

  /**
   * The build version.
   */
  public static final String BUILD_VERSION = getBuildProperty("xyzhub.version");

  /**
   * Read a file either from "~/.xyz-hub" or from the resources. The location of home can be overridden using the environment variable
   * XYZ_CONFIG_PATH.
   *
   * @param filename the filename of the file to read, e.g. "auth/jwt.key".
   * @return the bytes of the file.
   * @throws IOException if the file does not exist or any other error occurred.
   */
  public static @Nonnull byte[] readFileFromHomeOrResource(@Nonnull String filename) throws IOException {
    //noinspection ConstantConditions
    if (filename == null) {
      throw new FileNotFoundException("null");
    }
    final char first = filename.charAt(0);
    if (first == '/' || first == '\\') {
      filename = filename.substring(1);
    }

    final String pathEnvName = JsonConfigFile.configPathEnvName(Config.class);
    final String path = JsonConfigFile.nullable(System.getenv(pathEnvName));
    final Path filePath;
    if (path != null) {
      filePath = Paths.get(path, filename).toAbsolutePath();
    } else {
      final String userHome = System.getProperty("user.home");
      if (userHome != null) {
        filePath = Paths.get(userHome, ".config", "xyz-hub", filename).toAbsolutePath();
      } else {
        filePath = null;
      }
    }
    if (filePath != null) {
      final File file = filePath.toFile();
      if (file.exists() && file.isFile() && file.canRead()) {
        return Files.readAllBytes(filePath);
      }
    }

    try (final InputStream is = Core.class.getClassLoader().getResourceAsStream(filename)) {
      if (is == null) {
        throw new FileNotFoundException(filename);
      }
      return readNBytes(is, Integer.MAX_VALUE);
    }
  }

  private static final int DEFAULT_BUFFER_SIZE = 8192;
  private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

  // Taken from JDK 9+
  private static byte[] readNBytes(final InputStream is, int len) throws IOException {
    if (len < 0) {
      throw new IllegalArgumentException("len < 0");
    }

    List<byte[]> bufs = null;
    byte[] result = null;
    int total = 0;
    int remaining = len;
    int n;
    do {
      byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
      int nread = 0;

      // read to EOF which may read more or less than buffer size
      while ((n = is.read(buf, nread,
          Math.min(buf.length - nread, remaining))) > 0) {
        nread += n;
        remaining -= n;
      }

      if (nread > 0) {
        if (MAX_BUFFER_SIZE - total < nread) {
          throw new OutOfMemoryError("Required array size too large");
        }
        if (nread < buf.length) {
          buf = Arrays.copyOfRange(buf, 0, nread);
        }
        total += nread;
        if (result == null) {
          result = buf;
        } else {
          if (bufs == null) {
            bufs = new ArrayList<>();
            bufs.add(result);
          }
          bufs.add(buf);
        }
      }
      // if the last call to read returned -1 or the number of bytes
      // requested have been read then break
    } while (n >= 0 && remaining > 0);

    if (bufs == null) {
      if (result == null) {
        return new byte[0];
      }
      return result.length == total ?
          result : Arrays.copyOf(result, total);
    }

    result = new byte[total];
    int offset = 0;
    remaining = total;
    for (byte[] b : bufs) {
      int count = Math.min(b.length, remaining);
      System.arraycopy(b, 0, result, offset, count);
      offset += count;
      remaining -= count;
    }

    return result;
  }

  public static void initialize(
      @Nonnull VertxOptions vertxOptions,
      boolean debug,
      @Nonnull String configFilename,
      @Nonnull Handler<JsonObject> handler
  ) {
    final String pathEnvName = JsonConfigFile.configPathEnvName(Config.class);
    final String path = JsonConfigFile.nullable(System.getenv(pathEnvName));
    if (path != null) {
      configFilename = Paths.get(path, configFilename).toAbsolutePath().toString();
    } else {
      final String userHome = System.getProperty("user.home");
      if (userHome != null) {
        final String test =
            userHome + File.separatorChar + ".config" + File.separatorChar + "xyz-hub" + File.separatorChar + configFilename;
        final File configFile = new File(test);
        if (configFile.exists() && configFile.isFile()) {
          configFilename = configFile.toPath().toAbsolutePath().toString();
          if (!configFile.canRead()) {
            die(1, "Unable to access configuration file: " + configFilename);
          }
        }
      }
    }
    logger.info("Config file location: {}", configFilename);

    Configurator.initialize("default", CONSOLE_LOG_CONFIG);
    final ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", configFilename));
    final ConfigStoreOptions envConfig = new ConfigStoreOptions().setType("env");
    final ConfigStoreOptions sysConfig = new ConfigStoreOptions().setType("sys");
    final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore).addStore(envConfig).addStore(sysConfig)
        .setScanPeriod(24 * 60 * 1000);

    vertxOptions = (vertxOptions != null ? vertxOptions : new VertxOptions())
        .setWorkerPoolSize(NumberUtils.toInt(System.getenv(Core.VERTX_WORKER_POOL_SIZE), 128))
        .setPreferNativeTransport(true);

    if (debug) {
      vertxOptions
          .setBlockedThreadCheckInterval(TimeUnit.MINUTES.toMillis(1))
          .setMaxEventLoopExecuteTime(TimeUnit.MINUTES.toMillis(1))
          .setMaxWorkerExecuteTime(TimeUnit.MINUTES.toMillis(1))
          .setWarningExceptionTime(TimeUnit.MINUTES.toMillis(1));
    }

    vertx = Vertx.vertx(vertxOptions);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig(c -> {
      if (c.failed() || c.result() == null) {
        System.err.println("Unable to load the configuration.");
        System.exit(1);
      }
      JsonObject config = c.result();
      config.forEach(entry -> {
        if (entry.getValue() instanceof String) {
          if (entry.getValue().equals("")) {
            config.put(entry.getKey(), (String) null);
          } else {
            try {
              config.put(entry.getKey(), decryptSecret((String) entry.getValue()));
            } catch (final CryptoException e) {
              die(1, "Unable to decrypt value for key " + entry.getKey(), e);
            }
          }
        }
      });
      initializeLogger(config, debug);
      handler.handle(config);
    });
  }

  private static void initializeLogger(JsonObject config, boolean debug) {
    if (!CONSOLE_LOG_CONFIG.equals(config.getString("LOG_CONFIG"))) {
      Configurator.reconfigure(NetUtils.toURI(config.getString("LOG_CONFIG")));
    }
    if (debug) {
      changeLogLevel("DEBUG");
    }
  }

  static void changeLogLevel(String level) {
    logger.info("LOG LEVEL UPDATE requested. New level will be: " + level);

    Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.getLevel(level));

    logger.info("LOG LEVEL UPDATE performed. New level is now: " + level);
  }

  private static String decryptSecret(String encryptedSecret) throws CryptoException {
    if (ConfigDecryptor.isEncrypted(encryptedSecret)) {
      return ConfigDecryptor.decryptSecret(encryptedSecret);
    }
    return encryptedSecret;
  }

  public static final ThreadFactory newThreadFactory(String groupName) {
    return new DefaultThreadFactory(groupName);
  }

  private static class DefaultThreadFactory implements ThreadFactory {

    private ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public DefaultThreadFactory(String groupName) {
      assert groupName != null;
      group = new ThreadGroup(groupName);
      namePrefix = groupName + "-";
    }

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
    }
  }

  private static long getBuildTime() {
    String buildTime = getBuildProperty("xyzhub.buildTime");
    try {
      return new SimpleDateFormat("yyyy.MM.dd-HH:mm").parse(buildTime).getTime();
    } catch (ParseException e) {
      return 0;
    }
  }

  protected static String getBuildProperty(String name) {
    InputStream input = AbstractHttpServerVerticle.class.getResourceAsStream("/build.properties");

    // load a properties file
    Properties buildProperties = new Properties();
    try {
      buildProperties.load(input);
    } catch (IOException ignored) {
    }

    return buildProperties.getProperty(name);
  }

  public static void die(final int exitCode, final @Nonnull String reason) {
    die(exitCode, reason, new RuntimeException());
  }

  public static void die(
      final int exitCode,
      final @Nonnull String reason,
      @Nullable Throwable exception
  ) {
    // Let's always generate a stack-trace.
    if (exception == null) {
      exception = new RuntimeException();
    }
    logger.error(reason, exception);
    System.out.flush();
    System.err.println(reason);
    exception.printStackTrace(System.err);
    System.err.flush();
    System.exit(exitCode);
  }
}