package it.eng.spagobi.rest.interceptors;

import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.utilities.GeneralUtilities;
import it.eng.spagobi.commons.utilities.UserUtilities;
import it.eng.spagobi.services.proxy.SecurityServiceProxy;
import it.eng.spagobi.services.rest.AbstractSecurityServerInterceptor;
import it.eng.spagobi.services.security.bo.SpagoBIUserProfile;
import it.eng.spagobi.services.security.service.ISecurityServiceSupplier;
import it.eng.spagobi.services.security.service.SecurityServiceSupplierFactory;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.filters.FilterIOManager;

import java.lang.reflect.Method;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.axis.encoding.Base64;
import org.apache.log4j.Logger;
import org.jboss.resteasy.annotations.interception.Precedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;

/**
 * The org.jboss.resteasy.spi.interception.PreProcessInterceptor runs after a JAX-RS resource method is found to invoke on, but before the actual invocation
 * happens
 *
 * Similar to SpagoBIAccessFilter but designed for REST services
 *
 */
@Provider
@ServerInterceptor
@Precedence("SECURITY")
public class SecurityServerInterceptor extends AbstractSecurityServerInterceptor implements PreProcessInterceptor, AcceptedByMethod {

	static private Logger logger = Logger.getLogger(SecurityServerInterceptor.class);

	@Context
	private HttpServletRequest servletRequest;

	@Override
	protected ServerResponse notAuthenticated() {
		/*
		 * This response is standard in Basic authentication. If the header with credentials is missing the server send the response asking for the header. The
		 * browser will show a popup that requires the user credential.
		 */
		Headers<Object> header = new Headers<Object>();
		header.add("WWW-Authenticate", "Basic realm='spagobi'");
		return new ServerResponse("", 401, header);
	}

	@Override
	protected UserProfile authenticateUser() {
		UserProfile profile = null;

		logger.trace("IN");

		try {
			String encodedUserPassword = servletRequest.getHeader("Authorization").replaceFirst("Basic ", "");
			String credentials = null;
			byte[] decodedBytes = Base64.decode(encodedUserPassword);
			credentials = new String(decodedBytes, "UTF-8");

			StringTokenizer tokenizer = new StringTokenizer(credentials, ":");
			String user = tokenizer.nextToken();
			String password = tokenizer.nextToken();

			ISecurityServiceSupplier supplier = SecurityServiceSupplierFactory.createISecurityServiceSupplier();

			SpagoBIUserProfile spagoBIUserProfile = supplier.checkAuthentication(user, password);
			if (spagoBIUserProfile != null) {
				profile = (UserProfile) UserUtilities.getUserProfile(user);
			}
		} catch (Throwable t) {
			// Do nothing: it will return null
		} finally {
			logger.trace("OUT");
		}

		return profile;
	}

	@Override
	protected boolean isUserAuthenticatedInSpagoBI() {

		boolean authenticated = true;

		IEngUserProfile engProfile = getUserProfileFromSession();

		if (engProfile != null) {
			// verify if the profile stored in session is still valid
			String userId = null;
			try {
				userId = getUserIdentifier();
			} catch (Exception e) {
				logger.debug("User identifier not found");
				throw new SpagoBIRuntimeException("User identifier not found", e);
			}
			if (userId != null && userId.equals(engProfile.getUserUniqueIdentifier().toString()) == false) {
				logger.debug("User is authenticated but the profile store in session need to be updated");
				engProfile = this.getUserProfileFromUserId();
			} else {
				logger.debug("User is authenticated and his profile is already stored in session");
			}

		} else {
			engProfile = this.getUserProfileFromUserId();
			if (engProfile != null) {
				logger.debug("User is authenticated but his profile is not already stored in session");
			} else {
				logger.debug("User is not authenticated");
				authenticated = false;
			}
		}

		return authenticated;
	}

	@Override
	protected IEngUserProfile createProfile(String userId) {
		SecurityServiceProxy proxy = new SecurityServiceProxy(userId, servletRequest.getSession());

		try {
			return GeneralUtilities.createNewUserProfile(userId);
		} catch (Exception e) {
			logger.error("Error while creating user profile with user id = [" + userId + "]", e);
			throw new SpagoBIRuntimeException("Error while creating user profile with user id = [" + userId + "]", e);
		}
	}

	@Override
	protected IEngUserProfile getUserProfileFromSession() {
		IEngUserProfile engProfile = null;

		engProfile = (IEngUserProfile) servletRequest.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
		if (engProfile == null) {
			FilterIOManager ioManager = new FilterIOManager(servletRequest, null);
			ioManager.initConetxtManager();
			engProfile = (IEngUserProfile) ioManager.getContextManager().get(IEngUserProfile.ENG_USER_PROFILE);
			servletRequest.getSession().setAttribute(IEngUserProfile.ENG_USER_PROFILE, engProfile);
		}

		return engProfile;
	}

	@Override
	protected void setUserProfileInSession(IEngUserProfile engProfile) {
		super.setUserProfileInSession(engProfile);

		servletRequest.getSession().setAttribute(IEngUserProfile.ENG_USER_PROFILE, engProfile);
	}

	public boolean accept(Class declaring, Method method) {
		// return !method.isAnnotationPresent(POST.class);
		return true;
	}
}
