package org.springframework.cloud.sleuth.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.cloud.sleuth.Span;
import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper=false)
@SuppressWarnings("serial")
public class SpanStoppedEvent extends ApplicationEvent {

	private final Span span;
	private final Span parent;

	public SpanStoppedEvent(Object source, Span span) {
		this(source, null, span);
	}

	public SpanStoppedEvent(Object source, Span parent, Span span) {
		super(source);
		this.parent = parent;
		this.span = span;
	}
}
