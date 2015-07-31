package org.ats.service.report.jmeter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.ats.service.report.Report;
import org.ats.services.executor.ExecutorService;
import org.ats.services.executor.job.PerformanceJob;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class JtlHandler extends DefaultHandler {
  
  private String parsingType = "STRING";
  private String urlTotal = "*SummaryReport*";
  private Map<String, Report> totalUrlMap = new HashMap<String, Report>();
  private String content = "";
  private String performaneJobId;
  private String scriptId;
  private ExecutorService executorService;
  private String tmpValue;
  private Report reportAll;
  private Report reportUrl;
  private boolean isInAssertion = false;
  private boolean failureValue = true;
  private boolean errorValue = true;

  @Inject
  public JtlHandler(ExecutorService executorService, @Assisted("performaneJobId") String performaneJobId,  @Assisted("scriptId") String scriptId) {
    this.executorService = executorService;
    this.performaneJobId = performaneJobId;
    this.scriptId = scriptId;
    PerformanceJob job = (PerformanceJob) this.executorService.get(performaneJobId);
    Iterator<Map.Entry<String, String>> iterator = job.getRawDataOutput().entrySet().iterator();
    while(iterator.hasNext()){
      Map.Entry<String, String> entry = iterator.next();
      if(entry.getKey().equals(scriptId)){
        this.content = entry.getValue();
        break;
      }
    }
    
  }

  public void startParsing() throws SAXException, IOException, ParserConfigurationException {
    if (content.isEmpty()) {
      throw new ParserConfigurationException("Error content parsing is empty");
    }
    if ("STRING".equalsIgnoreCase(parsingType)) {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      InputStream ins = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
      saxParser.parse(ins, this);
      try {
        ins.close();
      } catch (Exception e) {
        throw e;
      }
    }

  }

  public Map<String, Report> getTotalUrlMap() {
    return totalUrlMap;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (qName.equalsIgnoreCase("httpSample")) {
      HttpSamplerObj httpSamplerObj = new HttpSamplerObj(attributes);
      reportAll = totalUrlMap.get(urlTotal);
      if (reportAll == null) {
        reportAll = new Report(urlTotal, performaneJobId, null,scriptId );
        reportAll.setSummaryReport(new SummaryReport(urlTotal));
        reportAll.setTransPersecond(new TreeMap<Long, PointReport>());
        reportAll.setHitPerSecond(new TreeMap<Long, PointReport>());
        totalUrlMap.put(urlTotal, reportAll);
      }
      Calculation.calculate(reportAll, httpSamplerObj);
      String url = httpSamplerObj.getLable();
      reportUrl = totalUrlMap.get(url);
      if (reportUrl == null) {
        reportUrl = new Report(url, performaneJobId, null,scriptId);
        reportUrl.setSummaryReport(new SummaryReport(url));
        reportUrl.setTransPersecond(new TreeMap<Long, PointReport>());
        reportUrl.setHitPerSecond(new TreeMap<Long, PointReport>());
        totalUrlMap.put(url, reportUrl);
      }
      Calculation.calculate(reportUrl, httpSamplerObj);
    }
    if (qName.equalsIgnoreCase("assertionResult")) {
      isInAssertion = true;
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (qName.equalsIgnoreCase("failure")) {
      if (isInAssertion) {
        if ("false".equalsIgnoreCase(tmpValue)) {
          failureValue = false;
        }
      }
    }
    if (qName.equalsIgnoreCase("error")) {
      if (isInAssertion) {
        if ("false".equalsIgnoreCase(tmpValue)) {
          errorValue = false;
        }
      }
    }
    if (qName.equalsIgnoreCase("assertionResult")) {
      Calculation.calculateAssertion(reportAll, failureValue, errorValue);
      Calculation.calculateAssertion(reportUrl, failureValue, errorValue);
      isInAssertion = false;
    }
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    tmpValue = new String(ch, start, length);
  }

  public String getUrlTotal() {
    return urlTotal;
  }

  public void setUrlTotal(String urlTotal) {
    this.urlTotal = urlTotal;
  }

  public void setTotalUrlMap(Map<String, Report> totalUrlMap) {
    this.totalUrlMap = totalUrlMap;
  }
}
