/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.sleuth.instrument.web.client;

import static org.springframework.cloud.sleuth.Trace.PARENT_ID_NAME;
import static org.springframework.cloud.sleuth.Trace.PROCESS_ID_NAME;
import static org.springframework.cloud.sleuth.Trace.SPAN_ID_NAME;
import static org.springframework.cloud.sleuth.Trace.SPAN_NAME_NAME;
import static org.springframework.cloud.sleuth.Trace.TRACE_ID_NAME;
import static org.springframework.cloud.sleuth.TraceContextHolder.getCurrentSpan;
import static org.springframework.cloud.sleuth.TraceContextHolder.isTracing;

import java.io.IOException;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.event.ClientReceivedEvent;
import org.springframework.cloud.sleuth.event.ClientSentEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Interceptor that verifies whether the trance and span id has been set on the request
 * and sets them if one or both of them are missing.
 *
 * @see org.springframework.web.client.RestTemplate
 * @see Trace
 *
 * @author Marcin Grzejszczak, 4financeIT
 * @author Spencer Gibb
 */
public class TraceRestTemplateInterceptor implements ClientHttpRequestInterceptor,
ApplicationEventPublisherAware {

	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		setHeader(request, SPAN_ID_NAME, getCurrentSpan().getSpanId());
		setHeader(request, TRACE_ID_NAME, getCurrentSpan().getTraceId());
		setHeader(request, SPAN_NAME_NAME, getCurrentSpan().getName());
		String parentId = getParentId(getCurrentSpan());
		if (parentId != null) {
			setHeader(request, PARENT_ID_NAME, parentId);
		}
		String processId = getCurrentSpan().getProcessId();
		if (processId != null) {
			setHeader(request, PROCESS_ID_NAME, processId);
		}
		publish(new ClientSentEvent(this, getCurrentSpan()));
		return new TraceHttpResponse(this, execution.execute(request, body));
	}

	public void close() {
		publish(new ClientReceivedEvent(this, getCurrentSpan()));
	}

	private void publish(ApplicationEvent event) {
		if (this.publisher !=null) {
			this.publisher.publishEvent(event);
		}
	}

	private String getParentId(Span span) {
		return span.getParents() != null && !span.getParents().isEmpty() ? span
				.getParents().get(0) : null;
	}

	public void setHeader(HttpRequest request, String name, String value) {
		if (!request.getHeaders().containsKey(name) && isTracing()) {
			request.getHeaders().add(name, value);
		}
	}

}
