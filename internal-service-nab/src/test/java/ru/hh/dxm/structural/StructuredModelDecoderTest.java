package ru.hh.dxm.structural;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.dxm.DxmTest;
import ru.hh.util.trees.ExternalTree;

public class StructuredModelDecoderTest {
  @Test
  public void testUnmarshalSimpleTree() throws XMLStreamException {
    ExternalTree bTree = ExternalTree.of(new ElementVisitor("/b"));
    ExternalTree cTree = ExternalTree.of(new ElementVisitor("/c"));
    ExternalTree dTree = ExternalTree.of(new ElementVisitor("/d"));

    ExternalTree docTree = ExternalTree.of(new ArtifactVisitor(),
            "b", bTree, "c", cTree, "d", dTree);
    ExternalTree tree = ExternalTree.of(new ArtifactVisitor(),
            "doc", docTree);

    SimpleStructureBuilder result = new SimpleStructureBuilder();
    Supplier<SimpleStructureBuilder> supplier = Suppliers.ofInstance(result);
    StructuredModelDecoder<Map<String, String>> decoder = new StructuredModelDecoder<Map<String, String>>(tree,
            supplier);

    XMLStreamReader in = DxmTest.xml("<xml><doc><b>Hello</b><c>Buh-bye</c><d/></doc></xml>");
    in.next();
    in.next();
    in.require(XMLEvent.START_ELEMENT, null, "doc");
    decoder.unmarshal(in);

    Assert.assertEquals("Hello", result.get().get("/b"));
    Assert.assertEquals("Buh-bye", result.get().get("/c"));
    Assert.assertEquals("", result.get().get("/d"));
  }

  private static class SimpleStructureBuilder implements StructureBuilder<Map<String, String>> {
    private final Map<String, String> store = Maps.newHashMap();

    public void set(String path, String text) {
      store.put(path, text);
    }

    @Override
    public Map<String, String> get() {
      return store;
    }
  }

  private static class ArtifactVisitor implements StructureBuildingVisitor<SimpleStructureBuilder> {
    @Override
    public boolean visit(SimpleStructureBuilder mapping, XMLStreamReader in) throws XMLStreamException {
      in.next();
      return false;
    }
  }

  private static class ElementVisitor implements StructureBuildingVisitor<SimpleStructureBuilder> {
    private final String path;

    private ElementVisitor(String path) {
      this.path = path;
    }

    @Override
    public boolean visit(SimpleStructureBuilder mapping, XMLStreamReader in) throws XMLStreamException {
      mapping.set(path, in.getElementText());
      in.next();
      return true;
    }
  }
}
