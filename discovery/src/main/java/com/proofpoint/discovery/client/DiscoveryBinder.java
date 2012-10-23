/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.discovery.client;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.proofpoint.discovery.client.ServiceAnnouncement.ServiceAnnouncementBuilder;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;
import static com.proofpoint.discovery.client.ServiceAnnouncement.serviceAnnouncement;
import static com.proofpoint.discovery.client.ServiceTypes.serviceType;

public class DiscoveryBinder
{
    public static DiscoveryBinder discoveryBinder(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null");
        return new DiscoveryBinder(binder);
    }

    private final Multibinder<ServiceAnnouncement> serviceAnnouncementBinder;
    private final Binder binder;

    protected DiscoveryBinder(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null");
        this.binder = binder;
        this.serviceAnnouncementBinder = Multibinder.newSetBinder(binder, ServiceAnnouncement.class);
    }

    public void bindSelector(String type)
    {
        Preconditions.checkNotNull(type, "type is null");
        bindSelector(serviceType(type));
    }

    public void bindSelector(ServiceType serviceType)
    {
        Preconditions.checkNotNull(serviceType, "serviceType is null");
        bindConfig(binder).annotatedWith(serviceType).prefixedWith("discovery." + serviceType.value()).to(ServiceSelectorConfig.class);
        binder.bind(ServiceSelector.class).annotatedWith(serviceType).toProvider(new ServiceSelectorProvider(serviceType.value())).in(Scopes.SINGLETON);
    }

    public void bindServiceAnnouncement(ServiceAnnouncement announcement)
    {
        Preconditions.checkNotNull(announcement, "announcement is null");
        serviceAnnouncementBinder.addBinding().toInstance(announcement);
    }

    public void bindServiceAnnouncement(Provider<ServiceAnnouncement> announcementProvider)
    {
        Preconditions.checkNotNull(announcementProvider, "announcementProvider is null");
        serviceAnnouncementBinder.addBinding().toProvider(announcementProvider);
    }

    public <T extends ServiceAnnouncement> void bindServiceAnnouncement(Class<? extends Provider<T>> announcementProviderClass)
    {
        Preconditions.checkNotNull(announcementProviderClass, "announcementProviderClass is null");
        serviceAnnouncementBinder.addBinding().toProvider(announcementProviderClass);
    }

    public ServiceAnnouncementBuilder bindHttpAnnouncement(String type)
    {
        ServiceAnnouncementBuilder serviceAnnouncementBuilder = serviceAnnouncement(type);
        bindServiceAnnouncement(new HttpAnnouncementProvider(serviceAnnouncementBuilder));
        return serviceAnnouncementBuilder;
    }

    public void bindHttpSelector(String type)
    {
        Preconditions.checkNotNull(type, "type is null");
        bindHttpSelector(serviceType(type));
    }

    public void bindHttpSelector(ServiceType serviceType)
    {
        Preconditions.checkNotNull(serviceType, "serviceType is null");
        bindSelector(serviceType);
        binder.bind(HttpServiceSelector.class).annotatedWith(serviceType).toProvider(new HttpServiceSelectorProvider(serviceType.value())).in(Scopes.SINGLETON);
    }

    static class HttpAnnouncementProvider implements Provider<ServiceAnnouncement>
    {
        private final ServiceAnnouncementBuilder builder;
        private AnnouncementHttpServerInfo httpServerInfo;

        public HttpAnnouncementProvider(ServiceAnnouncementBuilder serviceAnnouncementBuilder)
        {
            builder = serviceAnnouncementBuilder;
        }

        @Inject
        public void setAnnouncementHttpServerInfo(AnnouncementHttpServerInfo httpServerInfo)
        {
            this.httpServerInfo = httpServerInfo;
        }

        @Override
        public ServiceAnnouncement get()
        {
            if (httpServerInfo.getHttpUri() != null) {
                builder.addProperty("http", httpServerInfo.getHttpUri().toString());
                builder.addProperty("http-external", httpServerInfo.getHttpExternalUri().toString());
            }
            if (httpServerInfo.getHttpsUri() != null) {
                builder.addProperty("https", httpServerInfo.getHttpsUri().toString());
                builder.addProperty("https-external", httpServerInfo.getHttpsExternalUri().toString());
            }
            return builder.build();
        }
    }
}
