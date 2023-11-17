package com.here.naksha.lib.psql;

import static com.here.naksha.lib.psql.PostgresInstance.allInstances;
import static com.here.naksha.lib.psql.PostgresInstance.mutex;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.here.naksha.lib.psql.PsqlDataSource.SLF4JLogWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PostgresQL database instance with a connection pool attached. This instance will live as long as there are any references to it, what
 * includes open and idle, pending connections.
 */
public final class PsqlInstance {

  private static final Logger log = LoggerFactory.getLogger(PsqlInstance.class);

  /**
   * Returns the PostgresQL database instance singleton for the given configuration or creates a new one, should there be no one yet.
   *
   * @param config The PostgresQL database instance configuration.
   * @return The PostgresQL database instance singleton.
   */
  public static @NotNull PsqlInstance get(@NotNull PsqlInstanceConfig config) {
    mutex.lock();
    PsqlInstance psqlInstance = null;
    PostgresInstance instance;
    try {
      instance = allInstances.get(config);
      if (instance != null) {
        psqlInstance = (PsqlInstance) instance.getProxy();
      }
      if (psqlInstance == null) {
        psqlInstance = new PsqlInstance(config);
        PostgresInstance existing = allInstances.putIfAbsent(config, psqlInstance.postgresInstance);
        assert existing == null;
      }
    } finally {
      mutex.unlock();
    }
    return psqlInstance;
  }

  PsqlInstance(@NotNull PsqlInstanceConfig config) {
    this.postgresInstance = new PostgresInstance(this, config);
  }

  final @NotNull PostgresInstance postgresInstance;

  /**
   * Returns a new connection from the pool.
   *
   * @param applicationName              The application name to be used for the connection.
   * @param schema                       The schema to select.
   * @param fetchSize                    The default fetch-size to use.
   * @param connTimeoutInSeconds         The connection timeout, if a new connection need to be established.
   * @param sockedReadTimeoutInSeconds   The socket read-timeout to be used with the connection.
   * @param cancelSignalTimeoutInSeconds The signal timeout to be used with the connection.
   * @return The connection.
   * @throws SQLException If acquiring the connection failed.
   */
  public @NotNull PsqlConnection getConnection(
      @NotNull String applicationName,
      @NotNull String schema,
      int fetchSize,
      long connTimeoutInSeconds,
      long sockedReadTimeoutInSeconds,
      long cancelSignalTimeoutInSeconds) throws SQLException {
    return postgresInstance.getConnection(applicationName, schema, fetchSize, connTimeoutInSeconds, sockedReadTimeoutInSeconds,
        cancelSignalTimeoutInSeconds);
  }

  /**
   * Returns the medium latency to this instance.
   *
   * @param timeUnit The time-unit in which to return the latency.
   * @return The latency.
   */
  public long getMediumLatency(@NotNull TimeUnit timeUnit) {
    return postgresInstance.getMediumLatency(timeUnit);
  }

  /**
   * Forcefully overrides auto-detected medium latency.
   *
   * @param latency  The latency to set.
   * @param timeUnit The time-unit in which the latency was provided.
   */
  public void setMediumLatency(long latency, @NotNull TimeUnit timeUnit) {
    postgresInstance.setMediumLatency(latency, timeUnit);
  }

  /**
   * Forcefully overrides auto-detected medium latency.
   *
   * @param latency  The latency to set.
   * @param timeUnit The time-unit in which the latency was provided.
   * @return this.
   */
  public @NotNull PsqlInstance withMediumLatency(long latency, @NotNull TimeUnit timeUnit) {
    setMediumLatency(latency, timeUnit);
    return this;
  }

  /**
   * Returns the maximum bandwidth to the PostgresQL server instance in gigabit.
   *
   * @return The maximum bandwidth to the PostgresQL server instance in gigabit.
   */
  public long getMaxBandwidthInGbit() {
    return postgresInstance.getMaxBandwidthInGbit();
  }

  /**
   * Forcefully set the maximum bandwidth to the PostgresQL server instance in gigabit.
   *
   * @param maxBandwidthInGbit The bandwidth in gigabit.
   */
  public void setMaxBandwidthInGbit(long maxBandwidthInGbit) {
    postgresInstance.setMaxBandwidthInGbit(maxBandwidthInGbit);
  }

  /**
   * Forcefully set the maximum bandwidth to the PostgresQL server instance in gigabit.
   *
   * @param maxBandwidthInGbit The bandwidth in gigabit.
   * @return this.
   */
  public @NotNull PsqlInstance withMaxBandwidthInGbit(long maxBandwidthInGbit) {
    setMaxBandwidthInGbit(maxBandwidthInGbit);
    return this;
  }

}
