package com.here.xyz.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
public abstract class ContextAwareEvent<T extends ContextAwareEvent> extends Event<T> {

  @JsonInclude(Include.NON_DEFAULT)
  private SpaceContext context = SpaceContext.DEFAULT;

  public enum SpaceContext {
    EXTENSION,
    SUPER,
    DEFAULT;

    public static SpaceContext of(String value) {
      if (value == null) {
        return null;
      }
      try {
        return valueOf(value);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  }

  /**
   * @return The space context in which the command depicted by this event will be executed.
   *  In case of a space which extends another one, this value depicts whether to execute the action only on the extension or on
   *  the whole extending space.
   */
  public SpaceContext getContext() {
    return context;
  }

  public void setContext(SpaceContext context) {
    if (context == null)
      throw new NullPointerException("Context can not be null.");
    this.context = context;
  }

  public T withContext(SpaceContext context) {
    setContext(context);
    return (T) this;
  }

}
