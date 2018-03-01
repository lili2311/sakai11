/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.portal.charon.LoginHelper;
import org.sakaiproject.tool.api.Session;

import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.component.cover.HotReloadConfigurationService;
/**
 * 
 * @author ieb
 * @since Sakai 2.4
 * @version $Rev$
 * 
 */
public class ReLoginHandler extends BasePortalHandler
{
	private static final String URL_FRAGMENT = "relogin";

	public ReLoginHandler()
	{
		setUrlFragment(ReLoginHandler.URL_FRAGMENT);
	}

	@Override
	public int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		return doGet(parts, req, res, session);
	}

	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ("true".equals(HotReloadConfigurationService.getString("edu.nyu.classes.saml.force-shibboleth-login", "")) &&
		    ServerConfigurationService.getString("edu.nyu.classes.saml.ssoURL", null) != null) {
			return NEXT;
		}

		if ((parts.length == 2) && (parts[1].equals(ReLoginHandler.URL_FRAGMENT)))
		{
			try
			{
				LoginHelper.resetNavMinimizedCookie(res);

				// Note: here we send a null path, meaning we will NOT set it as
				// a possible return path
				// we expect we are in the middle of a login screen processing,
				// and it's already set (user login button is "ulogin") -ggolden
				portal.doLogin(req, res, session, null, false);

				return END;
			}
			catch (Exception ex)
			{
				throw new PortalHandlerException(ex);
			}
		}
		else
		{
			return NEXT;
		}
	}

}
