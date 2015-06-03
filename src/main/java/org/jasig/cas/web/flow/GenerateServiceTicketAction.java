/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.web.flow;

import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.ticket.TicketException;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.jasig.cas.web.support.WebUtils;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.springframework.util.StringUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Action to generate a service ticket for a given Ticket Granting Ticket and
 * Service.
 *
 * @author Scott Battaglia
 * @since 3.0.4
 */
public final class GenerateServiceTicketAction extends AbstractAction {

    /** Instance of CentralAuthenticationService. */
    @NotNull
    private CentralAuthenticationService centralAuthenticationService;

    @NotNull
    private CookieRetrievingCookieGenerator alaProxyAuthenticationCookieGenerator;

    @NotNull
    private TicketRegistry serviceTicketRegistry;

    @Override
    protected Event doExecute(final RequestContext context) {
        final Service service = WebUtils.getService(context);
        final String ticketGrantingTicket = WebUtils.getTicketGrantingTicketId(context);

        try {
            final String serviceTicketId = this.centralAuthenticationService
                .grantServiceTicket(ticketGrantingTicket,
                    service);
            WebUtils.putServiceTicketInRequestScope(context,
                serviceTicketId);

	    //
	    // Create ALA specific cookie that any ALA web application can read
	    //
	    final ServiceTicket st =
		(ServiceTicket)this.serviceTicketRegistry.getTicket(serviceTicketId);

	    final List<Authentication> authentications =
		st.getGrantingTicket().getChainedAuthentications();

	    final String email = authentications.get(authentications.size() - 1).getPrincipal().getId();

	    this.alaProxyAuthenticationCookieGenerator.addCookie(WebUtils.getHttpServletRequest(context),
								 WebUtils.getHttpServletResponse(context),
								 email);

            return success();
        } catch (final TicketException e) {
            if (isGatewayPresent(context)) {
		// TODO: what to do here ALA-Auth cookie-wise?
                return result("gateway");
            }
        }

        return error();
    }

    public void setCentralAuthenticationService(
        final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    public void setAlaProxyAuthenticationCookieGenerator(final CookieRetrievingCookieGenerator alaProxyAuthenticationCookieGenerator) {
	this.alaProxyAuthenticationCookieGenerator = alaProxyAuthenticationCookieGenerator;
    }

    public void setServiceTicketRegistry(final TicketRegistry serviceTicketRegistry) {
	this.serviceTicketRegistry = serviceTicketRegistry;
    }

    protected boolean isGatewayPresent(final RequestContext context) {
        return StringUtils.hasText(context.getExternalContext()
            .getRequestParameterMap().get("gateway"));
    }
}
