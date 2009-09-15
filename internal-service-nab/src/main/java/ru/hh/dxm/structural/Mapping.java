package ru.hh.dxm.structural;

import com.google.common.base.Supplier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import ru.hh.dxm.Coder;
import ru.hh.dxm.Decoder;
import ru.hh.dxm.MarshalException;
import ru.hh.dxm.Marshaller;
import ru.hh.util.trees.ExternalTreeBuilder;

@Immutable
public final class Mapping {
  @Immutable
  public static class MappedElementDefinition<T> {
    private final Class<T> klass;
    private final String[] xmlPath;
    private final Coder<T> coder;
    private final Decoder<T> decoder;

    public MappedElementDefinition(Class<T> klass, String[] xmlPath, Marshaller<T> marshaller) {
      this(klass, xmlPath, marshaller, marshaller);
    }

    public MappedElementDefinition(Class<T> klass, String[] xmlPath, Coder<T> coder, Decoder<T> decoder) {
      this.klass = klass;
      this.xmlPath = xmlPath;
      this.coder = coder;
      this.decoder = decoder;
    }
  }

  @Immutable
  private static class MappedElement<T> {
    private final Class<T> klass;
    private final Coder<T> coder;
    private final Decoder<T> decoder;

    private MappedElement(Class<T> klass, Coder<T> coder, Decoder<T> decoder) {
      this.klass = klass;
      this.coder = coder;
      this.decoder = decoder;
    }
  }

  @NotThreadSafe
  private static class ReflectionStructureBuilder<T> implements StructureBuilder<T> {
    private final Object[] slots;
    private final Method factoryMethod;
    private final StructureBuildingVisitor[] visitors;

    public ReflectionStructureBuilder(final MappedElement[] mappings, Method factoryMethod,
                                      StructureBuildingVisitor[] visitors) {
      this.slots = new Object[mappings.length];
      this.factoryMethod = factoryMethod;
      this.visitors = visitors;
    }

    @Override
    public T get() {
      try {
        return (T) factoryMethod.invoke(null, slots);
      } catch (IllegalAccessException e) {
        throw new MarshalException(e);
      } catch (InvocationTargetException e) {
        throw new MarshalException(e);
      }
    }
  }

  private final MappedElement[] elements;
  private final StructuredModelDecoder decoder;
  private final ReflectionStructureBuildingVisitor[] visitors;

  @SuppressWarnings({"unchecked"})
  public Mapping(MappedElementDefinition[] elts, final Method factoryMethod) {
    elements = new MappedElement[elts.length];
    this.visitors = new ReflectionStructureBuildingVisitor[elements.length];
    ExternalTreeBuilder<String, BaseReflectionStructureBuildingVisitor> builder =
            new ExternalTreeBuilder<String, BaseReflectionStructureBuildingVisitor>(new ArtifactVisitor());
    for (int i = 0; i < elements.length; i++) {
      MappedElementDefinition def = elts[i];
      elements[i] = new MappedElement(def.klass, def.coder, def.decoder);
      visitors[i] = new ReflectionStructureBuildingVisitor(i);
      builder.add(def.xmlPath, visitors[i]);
    }
    Supplier<ReflectionStructureBuilder> builderFactory = new Supplier<ReflectionStructureBuilder>() {
      @Override
      public ReflectionStructureBuilder get() {
        return new ReflectionStructureBuilder(elements, factoryMethod, visitors);
      }
    };
    decoder = new StructuredModelDecoder(builder.build(), builderFactory);
  }

  public StructuredModelDecoder getDecoder() {
    return decoder;
  }

  private abstract class BaseReflectionStructureBuildingVisitor implements
          StructureBuildingVisitor<ReflectionStructureBuilder> {
  }

  private class ArtifactVisitor extends BaseReflectionStructureBuildingVisitor {
    @Override
    public boolean visit(ReflectionStructureBuilder mapping, XMLStreamReader in) throws XMLStreamException {
      in.next();
      return false;
    }
  }

  private class ReflectionStructureBuildingVisitor extends BaseReflectionStructureBuildingVisitor {
    private final int i;

    public ReflectionStructureBuildingVisitor(int i) {
      this.i = i;
    }

    @Override
    public boolean visit(ReflectionStructureBuilder mapping, XMLStreamReader in) throws XMLStreamException {
      mapping.slots[i] = elements[i].decoder.unmarshal(in);
      return true;
    }
  }
}
