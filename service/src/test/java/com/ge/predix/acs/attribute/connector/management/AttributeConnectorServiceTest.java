package com.ge.predix.acs.attribute.connector.management;

import java.util.Collections;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.connector.management.dao.AttributeAdapterConnectionEntity;
import com.ge.predix.acs.attribute.connector.management.dao.AttributeConnectorEntity;
import com.ge.predix.acs.attribute.connector.management.dao.ConnectorConverter;
import com.ge.predix.acs.encryption.Encryptor;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@Test
public class AttributeConnectorServiceTest {
    @InjectMocks
    private AttributeConnectorServiceImpl connectorService;
    @Mock
    private ZoneResolver zoneResolver;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private Encryptor encryptor;

    private final ConnectorConverter connectorConverter = new ConnectorConverter();

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn("my-secret").when(this.encryptor).encrypt(Mockito.anyString());
        Mockito.doReturn("my-secret").when(this.encryptor).decrypt(Mockito.anyString());
    }

    @Test
    public void testCreateResourceConnector() {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();

        AttributeConnector expectedConnector = new AttributeConnector();
        expectedConnector.setMaxCachedIntervalMinutes(100);
        expectedConnector.setAdapters(Collections.singleton(new AttributeAdapterConnection("http://my-endpoint.com",
                "http://my-uaa.com", "my-client", "my-secret")));
        Assert.assertTrue(this.connectorService.upsertResourceConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
    }

    @Test
     public void testUpdateResourceConnector() {
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setResourceAttributeConnector(new AttributeConnectorEntity());
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();

        AttributeConnector expectedConnector = new AttributeConnector();
        expectedConnector.setMaxCachedIntervalMinutes(100);
        expectedConnector.setAdapters(Collections.singleton(new AttributeAdapterConnection("http://my-endpoint.com",
                "http://my-uaa.com", "my-client", "my-secret")));
        Assert.assertFalse(this.connectorService.upsertResourceConnector(expectedConnector));
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
     }

    @Test(expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertResourceConnectorWhenSaveFails() {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));

        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(Collections.singleton(new AttributeAdapterConnection("http://my-endpoint.com",
                "http://my-uaa.com", "my-client", "my-secret")));
        this.connectorService.upsertResourceConnector(connector);
    }

    @Test(dataProvider = "badAdapterProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertResourceConnectorWhenAdapterValidationFails(final String endpointUrl,
            final String uaaTokenUrl, final String uaaClientId, final String uaaClientSecret) {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();

        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(100);
        connector.setAdapters(Collections
                .singleton(new AttributeAdapterConnection(endpointUrl, uaaTokenUrl, uaaClientId, uaaClientSecret)));
        this.connectorService.upsertResourceConnector(connector);
    }

    @Test(dataProvider = "badConnectorProvider", expectedExceptions = { AttributeConnectorException.class })
    public void testUpsertResourceConnectorWhenConnectorValidationFails(final AttributeConnector connector) {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();
        this.connectorService.upsertResourceConnector(connector);
    }

    @Test
    public void testGetResouceConnector() {
        ZoneEntity zoneEntity = new ZoneEntity();
        AttributeConnectorEntity connectorEntity = new AttributeConnectorEntity();
        connectorEntity.setCachedIntervalMinutes(100);
        connectorEntity.setAttributeAdapterConnections(Collections.singleton(new AttributeAdapterConnectionEntity(
                connectorEntity, "http://my-endpoint", "http://my-uaa", "my-client", "my-secret")));
        zoneEntity.setResourceAttributeConnector(connectorEntity);
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();

        AttributeConnector expectedConnector = this.connectorConverter.toConnector(connectorEntity);
        Assert.assertEquals(this.connectorService.retrieveResourceConnector(), expectedConnector);
    }

    @Test
    public void testGetResouceConnectorWhichDoesNotExist() {
        Mockito.doReturn(new ZoneEntity()).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertNull(this.connectorService.retrieveResourceConnector());
    }

    @Test
    public void testDeleteResourceConnector() {
        ZoneEntity zoneEntity = new ZoneEntity();
        AttributeConnectorEntity connectorEntity = new AttributeConnectorEntity();
        connectorEntity.setCachedIntervalMinutes(100);
        zoneEntity.setResourceAttributeConnector(connectorEntity);

        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertTrue(this.connectorService.deleteResourceConnector());
        Assert.assertNull(this.connectorService.retrieveResourceConnector());
    }

    @Test
    public void testDeleteResourceConnectorWhichDoesNotExist() {
        ZoneEntity zoneEntity = new ZoneEntity();
        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Assert.assertFalse(this.connectorService.deleteResourceConnector());
    }

    @Test(expectedExceptions = { AttributeConnectorException.class })
    public void testDeleteResourceConnectorWhenSaveFails() {
        ZoneEntity zoneEntity = new ZoneEntity();
        AttributeConnectorEntity connectorEntity = new AttributeConnectorEntity();
        zoneEntity.setResourceAttributeConnector(connectorEntity);

        Mockito.doReturn(zoneEntity).when(this.zoneResolver).getZoneEntityOrFail();
        Mockito.doThrow(Exception.class).when(this.zoneRepository).save(Mockito.any(ZoneEntity.class));
        this.connectorService.deleteResourceConnector();
    }

    @DataProvider(name = "badAdapterProvider")
    private Object[][] badAdapterProvider() {
        return new String[][] { { "", "http://my-uaa.com", "my-client", "my-secret" },
                { "http://my-endpoint", "", "my-client", "my-secret" },
                { "http://my-endpoint", "http://my-uaa.com", "", "my-secret" },
                { "http://my-endpoint", "http://my-uaa.com", "my-client", "" } };
    }

    @DataProvider(name = "badConnectorProvider")
    private Object[][] badConnectorProvider() {
        return new Object[][] { getNullConncetor(), getConnectorWithoutAdapter(),
                getConnectorWithCachedIntervalBelowThreshold() };
    }

    private Object[] getConnectorWithCachedIntervalBelowThreshold() {
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(10);
        connector.setAdapters(Collections.singleton(new AttributeAdapterConnection()));
        return new Object[] { connector };
    }

    private Object[] getConnectorWithoutAdapter() {
        AttributeConnector connector = new AttributeConnector();
        connector.setAdapters(Collections.singleton(new AttributeAdapterConnection()));
        return new Object[] { connector };
    }

    private Object[] getNullConncetor() {
        return new Object[] { null };
    }
}
