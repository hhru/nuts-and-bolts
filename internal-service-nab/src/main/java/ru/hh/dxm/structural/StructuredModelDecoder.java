package ru.hh.dxm.structural;

import com.google.common.base.Supplier;
import javax.annotation.concurrent.Immutable;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import ru.hh.dxm.Decoder;
import ru.hh.dxm.XmlUtils;
import ru.hh.util.trees.ExternalTree;
import ru.hh.util.trees.TreeNavigator;

/*
 a -> artifact
 a/b -> decoder + visitor
 a/c -> decoder + visitor
 a/d -> artifact
 a/d/e -> decoder + visitor
 ...
 etc
 */
@Immutable
public class StructuredModelDecoder<T> implements Decoder<T> {
  private final ExternalTree<String, ? extends StructureBuildingVisitor> tree;
  private final Supplier<? extends StructureBuilder<T>> builderFactory;

  public StructuredModelDecoder(ExternalTree<String, ? extends StructureBuildingVisitor> tree,
                                Supplier<? extends StructureBuilder<T>> builderFactory) {
    this.tree = tree;
    this.builderFactory = builderFactory;
  }

  @Override
  public T unmarshal(XMLStreamReader in) throws XMLStreamException {
    TreeNavigator<String, StructureBuildingVisitor, ExternalTree<String, StructureBuildingVisitor>> nav =
            new TreeNavigator(tree);
    StructureBuilder<T> mapping = builderFactory.get();

    do {
      if (in.getEventType() == XMLEvent.START_ELEMENT) {
        if (nav.tryDescend(in.getLocalName())) {
          if (nav.get().visit(mapping, in))
            nav.ascend();
        } else {
          XmlUtils.skipCurrentSubTree(in);
        }
      } else if (in.getEventType() == XMLEvent.END_ELEMENT) {
        if (!nav.ascend())
          break;
        in.next();
      }
    } while (true);

    return mapping.get();
  }
}
