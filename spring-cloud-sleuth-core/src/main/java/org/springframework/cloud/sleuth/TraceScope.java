package org.springframework.cloud.sleuth;

import java.io.Closeable;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;

import org.springframework.cloud.sleuth.event.SpanStoppedEvent;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Spencer Gibb
 */
@Value
@NonFinal
public class TraceScope implements Closeable {

	private final ApplicationEventPublisher publisher;

	/**
	 * the span for this scope
	 */
	private final Span span;

	/**
	 * the span that was "current" before this scope was entered
	 */
	private final Span savedSpan;

	@NonFinal
	private boolean detached = false;

	public TraceScope(ApplicationEventPublisher publisher, Span span, Span savedSpan) {
		this.publisher = publisher;
		this.span = span;
		this.savedSpan = savedSpan;
	}

	/**
	 * Remove this span as the current thread, but don't stop it yet or send it for
	 * collection. This is useful if the span object is then passed to another thread for
	 * use with Trace.continueTrace().
	 *
	 * @return the same Span object
	 */
	public Span detach() {
		if (this.detached) {
			ExceptionUtils.error("Tried to detach trace span but "
					+ "it has already been detached: " + this.span);
		}
		this.detached = true;

		Span cur = TraceContextHolder.getCurrentSpan();
		if (cur != this.span) {
			ExceptionUtils.error("Tried to detach trace span but "
					+ "it is not the current span for the '"
					+ Thread.currentThread().getName() + "' thread: " + this.span
					+ ". You have " + "probably forgotten to close or detach " + cur);
		}
		else {
			TraceContextHolder.setCurrentSpan(this.savedSpan);
		}
		return this.span;
	}

	@Override
	@SneakyThrows
	public void close() {
		if (this.detached) {
			return;
		}
		this.detached = true;
		Span cur = TraceContextHolder.getCurrentSpan();
		if (cur != this.span) {
			ExceptionUtils.error("Tried to close trace span but "
					+ "it is not the current span for the '"
					+ Thread.currentThread().getName() + "' thread" + this.span
					+ ".  You have " + "probably forgotten to close or detach " + cur);
		}
		else {
			this.span.stop();
			if (this.savedSpan != null
					&& this.span.getParents().contains(this.savedSpan.getSpanId())) {
				this.publisher.publishEvent(new SpanStoppedEvent(this, this.savedSpan,
						this.span));
			}
			else {
				this.publisher.publishEvent(new SpanStoppedEvent(this, this.span));
			}
			TraceContextHolder.setCurrentSpan(this.savedSpan);
		}
	}

}
