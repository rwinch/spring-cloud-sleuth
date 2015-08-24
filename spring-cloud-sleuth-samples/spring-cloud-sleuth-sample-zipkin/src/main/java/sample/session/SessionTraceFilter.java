/*
 * Copyright 2002-2015 the original author or authors.
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
package sample.session;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Rob Winch
 *
 */
@Component
@Order(SessionRepositoryFilter.DEFAULT_ORDER + 1)
public class SessionTraceFilter extends OncePerRequestFilter {
	static final String NAME = "spring.session.id";

	/* (non-Javadoc)
	 * @see org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			updateSessionId(request.getSession(false));
			filterChain.doFilter(new SessionTraceRequestWrapper(request), response);
		} finally {
			updateSessionId((String) null);
		}
	}

	static void updateSessionId(HttpSession session) {
		String id = session == null ? null : session.getId();
		updateSessionId(id);
	}

	static void updateSessionId(String id) {
		if(id == null) {
			return;
		}
		Span span = TraceContextHolder.getCurrentSpan();
		if(span != null) {
			span.addAnnotation(NAME, id);
		}
	}

	static class SessionTraceRequestWrapper extends HttpServletRequestWrapper {

		/**
		 * @param request
		 */
		public SessionTraceRequestWrapper(HttpServletRequest request) {
			super(request);
		}


		/* (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#changeSessionId()
		 */
		@Override
		public String changeSessionId() {
			String id = super.changeSessionId();
			updateSessionId(id);
			return id;
		}


		/* (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getSession(boolean)
		 */
		@Override
		public HttpSession getSession(boolean create) {
			HttpSession session = super.getSession(create);
			updateSessionId(session);
			return session;
		}

		/* (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getSession()
		 */
		@Override
		public HttpSession getSession() {
			return getSession(true);
		}

	}
}
