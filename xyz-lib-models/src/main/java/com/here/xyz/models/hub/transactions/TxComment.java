package com.here.xyz.models.hub.transactions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.here.xyz.INaksha;
import com.here.xyz.models.geojson.implementation.namespaces.XyzNamespace;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A transaction event that is a comment.
 */
@SuppressWarnings("unused")
@AvailableSince(INaksha.v2_0)
@JsonTypeName(value = "TxComment")
public class TxComment extends TxEvent {

  @AvailableSince(INaksha.v2_0)
  public static final String MESSAGE = "message";
  @AvailableSince(INaksha.v2_0)
  public static final String JSON = "json";
  @AvailableSince(INaksha.v2_0)
  public static final String ATTACHMENT = "attachment";

  /**
   * Create a new comment.
   *
   * @param id         the local identifier of the event.
   * @param storageId  the storage identifier.
   * @param collection the collection impacted.
   * @param txn        the transaction number.
   * @param message    the commit message.
   */
  @AvailableSince(INaksha.v2_0)
  @JsonCreator
  public TxComment(
      @JsonProperty(ID) @NotNull String id,
      @JsonProperty(STORAGE_ID) @NotNull String storageId,
      @JsonProperty(COLLECTION) @NotNull String collection,
      @JsonProperty(XyzNamespace.TXN) @NotNull String txn,
      @JsonProperty(MESSAGE) @NotNull String message
  ) {
    super(id, storageId, collection, txn);
    assert !id.equals(collection) && id.startsWith("msg:");
    this.message = message;
  }

  /**
   * The human-readable message.
   */
  @AvailableSince(INaksha.v2_0)
  @JsonProperty(MESSAGE)
  public @NotNull String message;

  /**
   * The JSON details; if any.
   */
  @AvailableSince(INaksha.v2_0)
  @JsonProperty(JSON)
  public @Nullable Object json;

  /**
   * A binary attachment; if any.
   */
  @AvailableSince(INaksha.v2_0)
  @JsonProperty(ATTACHMENT)
  public byte @Nullable [] attachment;
}
