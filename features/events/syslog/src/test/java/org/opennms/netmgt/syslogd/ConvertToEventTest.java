/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.syslogd;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.junit.Test;
import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.config.SyslogdConfigFactory;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.dao.api.MonitoringLocationDao;
import org.opennms.netmgt.dao.hibernate.InterfaceToNodeCacheDaoImpl;
import org.opennms.netmgt.dao.mock.MockInterfaceToNodeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * Convert to event junit test file to test the performance of Syslogd ConvertToEvent processor
 * @author ms043660
 */
public class ConvertToEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertToEventTest.class);

    /**
     * Test method which calls the ConvertToEvent constructor.
     * 
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     */
    @Test
    public void testConvertToEvent() throws MarshalException, ValidationException, IOException {

        InterfaceToNodeCacheDaoImpl.setInstance(new MockInterfaceToNodeCache());

        // 10000 sample syslogmessages from xml file are taken and passed as
        // Inputstream to create syslogdconfiguration
        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this,
                                                                              "/etc/syslogd-loadtest-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        // Sample message which is embedded in packet and passed as parameter
        // to
        // ConvertToEvent constructor
        byte[] bytes = "<34> 2010-08-19 localhost foo10000: load test 10000 on tty1".getBytes();

        // Datagram packet which is passed as parameter for ConvertToEvent
        // constructor
        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length,
                                                InetAddress.getLocalHost(),
                                                SyslogClient.PORT);
        String data = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(pkt.getData())).toString();

        // ConvertToEvent takes 4 parameter
        // @param addr The remote agent's address.
        // @param port The remote agent's port
        // @param data The XML data in US-ASCII encoding.
        // @param len The length of the XML data in the buffer
        try {
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                pkt.getAddress(),
                pkt.getPort(),
                data,
                config
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
    }

    @Test
    public void testCiscoEventConversion() throws MarshalException, ValidationException, IOException {

        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this, "/etc/syslogd-cisco-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        try {
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                InetAddressUtils.ONE_TWENTY_SEVEN,
                9999,
                "<190>Mar 11 08:35:17 aaa_host 30128311: Mar 11 08:35:16.844 CST: %SEC-6-IPACCESSLOGP: list in110 denied tcp 192.168.10.100(63923) -> 192.168.11.128(1521), 1 packet", 
                config
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
    }

    @Test
    public void testNms5984() throws MarshalException, ValidationException, IOException {

        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this, "/etc/syslogd-nms5984-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        try {
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                InetAddressUtils.ONE_TWENTY_SEVEN,
                9999,
                "<11>Jul 19 15:55:21 otrs-test OTRS-CGI-76[14364]: [Error][Kernel::System::ImportExport::ObjectBackend::CI2CILink::ImportDataSave][Line:468]: CILink: Could not create link between CIs!", 
                config
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
    }
}