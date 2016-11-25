/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.security.authentication;

import org.alfresco.repo.cache.SimpleCache;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mocked test for {@link AuthenticationServiceImpl}
 *
 * @author amukha
 * @since 5.2
 */
public class AuthenticationServiceImplTest
{
    private AuthenticationComponent authenticationComponent = mock(AuthenticationComponent.class);
    private SimpleCache<String, AuthenticationServiceImpl.ProtectedUser> cache;
    private TicketComponent ticketComponent = mock(TicketComponent.class);
    private AuthenticationServiceImpl authService;

    private static final String USERNAME = "username";
    private static final char[] PASSWORD = "password".toCharArray();

    @Before
    public void beforeTest()
    {
        authService = new AuthenticationServiceImpl();
        authService.setAuthenticationComponent(authenticationComponent);
        authService.setTicketComponent(ticketComponent);
        cache = new MockCache<>();
        authService.setProtectedUsersCache(cache);
    }

    @Test
    public void testProtectedUserBadPassword()
    {
        int attempts = 3;
        authService.setProtectionPeriodSeconds(99999);
        authService.setProtectionLimit(attempts);
        authService.setProtectionEnabled(true);

        doThrow(new AuthenticationException("Bad password"))
                .when(authenticationComponent).authenticate(USERNAME, PASSWORD);
        for (int i = 0; i < attempts + 3; i++)
        {
            try
            {
                authService.authenticate(USERNAME, PASSWORD);
                fail("The " + AuthenticationException.class.getName() + " should have been thrown.");
            }
            catch (AuthenticationException ae)
            {
                // normal
            }
        }
        verify(authenticationComponent, times(attempts)).authenticate(USERNAME, PASSWORD);
        assertTrue("The user should be protected.", cache.get(USERNAME).isProtected());
    }

    @Test
    public void testProtectedUserCanLoginAfterProtection() throws Exception
    {
        int timeLimit = 1;
        authService.setProtectionPeriodSeconds(timeLimit);
        authService.setProtectionLimit(2);
        authService.setProtectionEnabled(true);

        doThrow(new AuthenticationException("Bad password"))
                .when(authenticationComponent).authenticate(USERNAME, PASSWORD);
        for (int i = 0; i < 2; i++)
        {
            try
            {
                authService.authenticate(USERNAME, PASSWORD);
                fail("An " + AuthenticationException.class.getName() + " should be thrown.");
            }
            catch (AuthenticationException ae)
            {
                // normal
            }
        }
        assertTrue("The user should be protected.", cache.get(USERNAME).isProtected());
        Thread.sleep(timeLimit*1000 + 1);
        assertFalse("The user should not be protected any more.", cache.get(USERNAME).isProtected());

    }

    private class MockCache<K extends Serializable, V> implements SimpleCache<K,V>
    {
        private Map<K,V> internalCache;

        MockCache()
        {
            internalCache = new HashMap<>();
        }

        @Override
        public boolean contains(K key)
        {
            return internalCache.containsKey(key);
        }

        @Override
        public Collection<K> getKeys()
        {
            return internalCache.keySet();
        }

        @Override
        public V get(K key)
        {
            return internalCache.get(key);
        }

        @Override
        public void put(K key, V value)
        {
            internalCache.put(key, value);
        }

        @Override
        public void remove(K key)
        {
            internalCache.remove(key);
        }

        @Override
        public void clear()
        {
            internalCache.clear();
        }
    }
}
