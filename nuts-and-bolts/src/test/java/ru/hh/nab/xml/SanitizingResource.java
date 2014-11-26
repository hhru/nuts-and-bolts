package ru.hh.nab.xml;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("/")
@Singleton
public class SanitizingResource {

  public static final String BAD_XML_CONTENT = "Some text \u0007 some \ud83d\ude0a text";
  public static final String SANITIZED_XML_CONTENT = "Some text \uFFFD some \ud83d\ude0a text";

  List<Tag> putted = new ArrayList<Tag>();

  @GET
  @Path("/getRoot")
  public Tag getRoot() {
    return new Tag(BAD_XML_CONTENT);
  }

  @GET
  @Path("/getList")
  public List<Tag> getList() {
    return Arrays.asList(new Tag(BAD_XML_CONTENT));
  }

  @GET
  @Path("/getJAXB")
  public JAXBElement<Tag> getJAXB() {
    return new JAXBElement<Tag>(new QName(Tag.class.getSimpleName()), Tag.class, new Tag(BAD_XML_CONTENT));
  }

  @PUT
  @Path("/putRoot")
  public Response putRoot(Tag tag) {
    putted.add(tag);
    return Response.ok().build();
  }

  @PUT
  @Path("/putList")
  public Response putList(List<Tag> tagList) {
    if(tagList != null) {
      putted.addAll(tagList);
    }
    return Response.ok().build();
  }

  @PUT
  @Path("/putJAXB")
  public Response putJaxb(JAXBElement<Tag> tagElement) {
    putted.add(tagElement.getValue());
    return Response.ok().build();
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "tag")
  public static class Tag {
    @XmlAttribute
    String name;

    public Tag() {}

    public Tag(String name) {
      this.name = name;
    }
  }
}
