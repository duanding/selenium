// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.devtools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.devtools.v84.network.Network;
import org.openqa.selenium.devtools.v84.network.model.BlockedReason;
import org.openqa.selenium.devtools.v84.network.model.ConnectionType;
import org.openqa.selenium.devtools.v84.network.model.Cookie;
import org.openqa.selenium.devtools.v84.network.model.Headers;
import org.openqa.selenium.devtools.v84.network.model.InterceptionStage;
import org.openqa.selenium.devtools.v84.network.model.RequestId;
import org.openqa.selenium.devtools.v84.network.model.RequestPattern;
import org.openqa.selenium.devtools.v84.network.model.ResourceType;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.testing.NotYetImplemented;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.devtools.v84.network.Network.clearBrowserCache;
import static org.openqa.selenium.devtools.v84.network.Network.clearBrowserCookies;
import static org.openqa.selenium.devtools.v84.network.Network.continueInterceptedRequest;
import static org.openqa.selenium.devtools.v84.network.Network.dataReceived;
import static org.openqa.selenium.devtools.v84.network.Network.deleteCookies;
import static org.openqa.selenium.devtools.v84.network.Network.disable;
import static org.openqa.selenium.devtools.v84.network.Network.emulateNetworkConditions;
import static org.openqa.selenium.devtools.v84.network.Network.enable;
import static org.openqa.selenium.devtools.v84.network.Network.eventSourceMessageReceived;
import static org.openqa.selenium.devtools.v84.network.Network.getAllCookies;
import static org.openqa.selenium.devtools.v84.network.Network.getCertificate;
import static org.openqa.selenium.devtools.v84.network.Network.getCookies;
import static org.openqa.selenium.devtools.v84.network.Network.getRequestPostData;
import static org.openqa.selenium.devtools.v84.network.Network.getResponseBody;
import static org.openqa.selenium.devtools.v84.network.Network.loadingFailed;
import static org.openqa.selenium.devtools.v84.network.Network.loadingFinished;
import static org.openqa.selenium.devtools.v84.network.Network.requestIntercepted;
import static org.openqa.selenium.devtools.v84.network.Network.requestServedFromCache;
import static org.openqa.selenium.devtools.v84.network.Network.requestWillBeSent;
import static org.openqa.selenium.devtools.v84.network.Network.resourceChangedPriority;
import static org.openqa.selenium.devtools.v84.network.Network.responseReceived;
import static org.openqa.selenium.devtools.v84.network.Network.searchInResponseBody;
import static org.openqa.selenium.devtools.v84.network.Network.setBlockedURLs;
import static org.openqa.selenium.devtools.v84.network.Network.setBypassServiceWorker;
import static org.openqa.selenium.devtools.v84.network.Network.setCacheDisabled;
import static org.openqa.selenium.devtools.v84.network.Network.setCookie;
import static org.openqa.selenium.devtools.v84.network.Network.setDataSizeLimitsForTest;
import static org.openqa.selenium.devtools.v84.network.Network.setExtraHTTPHeaders;
import static org.openqa.selenium.devtools.v84.network.Network.setRequestInterception;
import static org.openqa.selenium.devtools.v84.network.Network.setUserAgentOverride;
import static org.openqa.selenium.devtools.v84.network.Network.signedExchangeReceived;
import static org.openqa.selenium.devtools.v84.network.Network.webSocketClosed;
import static org.openqa.selenium.devtools.v84.network.Network.webSocketCreated;
import static org.openqa.selenium.devtools.v84.network.Network.webSocketFrameError;
import static org.openqa.selenium.devtools.v84.network.Network.webSocketFrameReceived;
import static org.openqa.selenium.devtools.v84.network.Network.webSocketFrameSent;
import static org.openqa.selenium.testing.drivers.Browser.CHROME;

public class ChromeDevToolsNetworkTest extends DevToolsTestBase {

  @Test
  public void getSetDeleteAndClearAllCookies() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    List<Cookie> allCookies = devTools.send(getAllCookies());

    assertEquals(0, allCookies.size());

    boolean setCookie = devTools.send(setCookie(
      "name",
      "value",
      Optional.of("http://localhost/devtools/test"),
      Optional.of("localhost"),
      Optional.of("/devtools/test"),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()));
    assertTrue(setCookie);

    assertEquals(1, devTools.send(getAllCookies()).size());
    assertEquals(0, devTools.send(getCookies(Optional.empty())).size());

    devTools.send(deleteCookies("name", Optional.empty(), Optional.of("localhost"),
                                Optional.of("/devtools/test")));

    devTools.send(clearBrowserCookies());

    assertEquals(0, devTools.send(getAllCookies()).size());

    setCookie = devTools.send(setCookie(
      "name",
      "value",
      Optional.of("http://localhost/devtools/test"),
      Optional.of("localhost"),
      Optional.of("/devtools/test"),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()));
    assertTrue(setCookie);

    assertEquals(1, devTools.send(getAllCookies()).size());
  }

  @Test
  @NotYetImplemented(CHROME)
  public void sendRequestWithUrlFiltersAndExtraHeadersAndVerifyRequests() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.send(setBlockedURLs(singletonList("*://*/*.css")));

    devTools.send(setExtraHTTPHeaders(new Headers(ImmutableMap.of("headerName", "headerValue"))));

    AtomicReference<BlockedReason> blockedReason = new AtomicReference<>();
    devTools.addListener(loadingFailed(), loadingFailed -> {
      if (loadingFailed.getType().equals(ResourceType.STYLESHEET)) {
        blockedReason.set(loadingFailed.getBlockedReason().get());
      }
    });

    AtomicReference<Object> header = new AtomicReference<>();
    devTools.addListener(requestWillBeSent(), requestWillBeSent ->
        header.set(requestWillBeSent.getRequest().getHeaders().get("headerName")));

    AtomicReference<RequestId> requestId = new AtomicReference<>();
    devTools.addListener(dataReceived(), dataReceived ->
        requestId.set(dataReceived.getRequestId()));

    driver.get(appServer.whereIs("js/skins/lightgray/content.min.css"));

    wait.until(d -> blockedReason.get() != null);
    wait.until(d -> header.get() != null);
    wait.until(d -> requestId.get() != null);

    assertEquals(blockedReason.get(), BlockedReason.INSPECTOR);
    assertEquals(header.get(),"headerValue");
  }

  @Test
  public void emulateNetworkConditionOffline() {
    devTools.send(enable(Optional.of(100000000), Optional.empty(), Optional.empty()));

    try {
      devTools.send(
          emulateNetworkConditions(true, 100, 1000, 2000, Optional.of(ConnectionType.CELLULAR3G)));

      AtomicReference<String> errorMessage = new AtomicReference<>();
      devTools.addListener(loadingFailed(),
                           loadingFailed -> errorMessage.set(loadingFailed.getErrorText()));

      try {
        driver.get(appServer.whereIs("simpleTest.html"));
      } catch (WebDriverException ignore) {
        // it can throw a WebDriverException with a message "net::ERR_INTERNET_DISCONNECTED"
      }
      wait.until(d -> errorMessage.get() != null);
      assertEquals(errorMessage.get(), "net::ERR_INTERNET_DISCONNECTED");
    } finally {
      devTools.send(
          emulateNetworkConditions(false, 0, -1, -1, Optional.of(ConnectionType.NONE)));
    }
  }

  @Test
  public void verifyRequestReceivedFromCacheAndResponseBody() {
    final RequestId[] requestIdFromCache = new RequestId[1];
    devTools.send(enable(Optional.empty(), Optional.of(100000000), Optional.empty()));

    devTools.addListener(requestServedFromCache(), requestId -> {
      Assert.assertNotNull(requestId);
      requestIdFromCache[0] = requestId;
    });

    devTools.addListener(loadingFinished(),
                         dataReceived -> Assert.assertNotNull(dataReceived.getRequestId()));

    driver.get(appServer.whereIs("simpleTest.html"));
    driver.get(appServer.whereIs("simpleTest.html"));

    Network.GetResponseBodyResponse responseBody = devTools.send(getResponseBody(requestIdFromCache[0]));
    Assert.assertNotNull(responseBody);
  }

  @Test
  public void verifySearchInResponseBody() {
    final RequestId[] requestIds = new RequestId[1];
    devTools.send(enable(Optional.empty(), Optional.of(100000000), Optional.empty()));

    devTools.addListener(responseReceived(), responseReceived -> {
      Assert.assertNotNull(responseReceived);
      Assert.assertNotNull(responseReceived.getResponse().getTiming());
      requestIds[0] = responseReceived.getRequestId();
    });

    driver.get(appServer.whereIs("simpleTest.html"));

    assertTrue(devTools.send(
      searchInResponseBody(requestIds[0], "/", Optional.empty(), Optional.empty())).size() > 0);
  }

  @Test
  public void verifyCacheDisabledAndClearCache() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.of(100000000)));

    driver.get(appServer.whereIs("simpleTest.html"));

    devTools.send(setCacheDisabled(true));

    devTools.addListener(responseReceived(), responseReceived ->
        assertEquals(false, responseReceived.getResponse().getFromDiskCache().get()));

    driver.get(appServer.whereIs("simpleTest.html"));

    devTools.send(clearBrowserCache());
  }

  @Test
  @NotYetImplemented(CHROME)
  public void verifyCertificatesAndOverrideUserAgent() {

    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.send(setUserAgentOverride("userAgent", Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(requestWillBeSent(),
                         requestWillBeSent -> assertEquals("userAgent",
                                                                  requestWillBeSent
                                                                      .getRequest()
                                                                      .getHeaders()
                                                                      .get("User-Agent")));
    driver.get(appServer.whereIsSecure("simpleTest.html"));

    assertThat(devTools.send(getCertificate(appServer.whereIsSecure("simpleTest.html")))).isNotEmpty();
  }

  @Test
  public void verifyResponseReceivedEventAndNetworkDisable() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));
    devTools.addListener(responseReceived(), Assert::assertNotNull);
    driver.get(appServer.whereIs("simpleTest.html"));
    devTools.send(disable());
  }

  @Test
  public void verifyWebSocketOperations() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(webSocketCreated(), Assert::assertNotNull);
    devTools.addListener(webSocketFrameReceived(), Assert::assertNotNull);
    devTools.addListener(webSocketClosed(), Assert::assertNotNull);
    devTools.addListener(webSocketFrameError(), Assert::assertNotNull);
    devTools.addListener(webSocketFrameSent(), Assert::assertNotNull);

    driver.get(appServer.whereIs("simpleTest.html"));
  }

  @Test
  public void verifyRequestPostData() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    final RequestId[] requestIds = new RequestId[1];

    devTools.addListener(requestWillBeSent(), requestWillBeSent -> {
      Assert.assertNotNull(requestWillBeSent);
      if (requestWillBeSent.getRequest().getMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
        requestIds[0] = requestWillBeSent.getRequestId();
      }
    });

    driver.get(appServer.whereIs("postForm.html"));

    driver.findElement(By.xpath("/html/body/form/input")).click();

    Assert.assertNotNull(devTools.send(getRequestPostData(requestIds[0])));
  }

  @Test
  public void byPassServiceWorker() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.send(setBypassServiceWorker(true));
  }

  @Test
  public void dataSizeLimitsForTest() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.send(setDataSizeLimitsForTest(10000, 100000));
  }

  @Test
  public void verifyEventSourceMessage() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(eventSourceMessageReceived(), Assert::assertNotNull);

    driver.get(appServer.whereIs("simpleTest.html"));
  }

  @Test
  public void verifySignedExchangeReceived() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(signedExchangeReceived(), Assert::assertNotNull);

    driver.get(appServer.whereIsSecure("simpleTest.html"));
  }

  @Test
  public void verifyResourceChangedPriority() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(resourceChangedPriority(), Assert::assertNotNull);

    driver.get(appServer.whereIsSecure("simpleTest.html"));
  }

  @Test
  public void interceptRequestAndContinue() {
    devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));

    devTools.addListener(requestIntercepted(),
                         requestIntercepted -> devTools.send(
                             continueInterceptedRequest(requestIntercepted.getInterceptionId(),
                                                        Optional.empty(),
                                                        Optional.empty(),
                                                        Optional.empty(), Optional.empty(),
                                                        Optional.empty(),
                                                        Optional.empty(), Optional.empty())));

    RequestPattern requestPattern = new RequestPattern(
      Optional.of("*.css"),
      Optional.of(ResourceType.STYLESHEET),
      Optional.of(InterceptionStage.HEADERSRECEIVED));
    devTools.send(setRequestInterception(ImmutableList.of(requestPattern)));

    driver.get(appServer.whereIs("js/skins/lightgray/content.min.css"));
  }

}
