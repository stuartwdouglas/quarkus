package io.quarkus.rest.test.xxe.resource.xxeJaxb;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class XxeJaxbFavoriteMovieXmlRootElement {
   private String _title;

   public String getTitle() {
      return _title;
   }

   public void setTitle(String title) {
      _title = title;
   }
}
