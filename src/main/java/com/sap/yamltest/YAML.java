package com.sap.yamltest;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

import java.text.NumberFormat;

import java.util.List;
import java.util.ArrayList;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.serializer.Serializer;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.serializer.AnchorGenerator;

public class YAML {
  private Node doc;
  private LoaderOptions lopts;
  private DumperOptions dopts;
  private Yaml yaml;

  public YAML(String input) throws IOException {
    this.lopts = new LoaderOptions();
    lopts.setProcessComments(true);

    this.dopts = new DumperOptions();
    dopts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    dopts.setCanonical(false);
    dopts.setProcessComments(true);
    dopts.setAnchorGenerator(new RetainAnchorGenerator());

    this.yaml = new Yaml(new Constructor(lopts),
                         new Representer(dopts),
                         dopts, lopts,
                         new Resolver());

    this.parse(input);
  }

  private void parse(String input) {
    this.doc = yaml.compose(new StringReader(input));
  }

  public String dump() throws IOException {
    // use serializer/emitter to dump
    StringWriter sw = new StringWriter();
    Serializer s = new Serializer(new Emitter(sw, this.dopts),
                                  new Resolver(),
                                  this.dopts, null);
    s.open();
    s.serialize(this.doc);
    s.close();
    return sw.toString();
  }

  public YAML setValue(String path, String value) throws Exception {
    String[] parts = path.split("\\.", 0);
    int nparts = parts.length;
    System.out.println("Parts: " + parts);

    Node node = this.doc;
    for (int i=0; i<nparts; ++i) {
      String part = parts[i];
      if ( node instanceof MappingNode ) {
        MappingNode parent = (MappingNode)node;
        boolean found = false;
        if ( i != (nparts-1) ) {
          // we're moving on, this will not be the last item
          for (NodeTuple nt: ((MappingNode)node).getValue()) {
            Node kn = nt.getKeyNode();
            if ( !(kn instanceof ScalarNode) )
              throw new Exception("Only scalar keynodes are supported");

            String key = ((ScalarNode)kn).getValue();
            if ( key.equals(part) ) {
              node = nt.getValueNode();
              found = true;
              break;
            }
            if ( !found )
              throw new Exception("Unable to find key: "+part);
          }

        } else {
          // this is the last part, we need to update the value here
          List<NodeTuple> newtuples = new ArrayList<NodeTuple>();

          for (NodeTuple nt: ((MappingNode)node).getValue()) {
            Node kn = nt.getKeyNode();
            if ( !(kn instanceof ScalarNode) )
              throw new Exception("Only scalar keynodes are supported");

            String key = ((ScalarNode)kn).getValue();
            if ( key.equals(part) ) {
              Node old = nt.getValueNode();
              if ( old instanceof ScalarNode ) {
                Node newval;
                ScalarNode sold = (ScalarNode)old;
                newval = new ScalarNode(sold.getTag(),
                                        false,
                                        value,
                                        sold.getStartMark(),
                                        sold.getEndMark(),
                                        sold.getScalarStyle());
                if ( sold.getAnchor() != null )
                  newval.setAnchor(sold.getAnchor());
                newtuples.add(new NodeTuple(kn, newval));
              } else {
                newtuples.add(nt);
              }
            } else {
              newtuples.add(nt);
            }
          }

          parent.setValue(newtuples);
          return this;
        }
      } else if ( node instanceof SequenceNode ) {
        System.out.println("doc nodetype: SequenceNode");
      } else if ( node instanceof CollectionNode ) {
        System.out.println("doc nodetype: CollectionNode");
      } else if ( node instanceof ScalarNode ) {
        System.out.println("doc nodetype: ScalarNode");
      } else  {
        System.out.println("doc nodetype: " + node);
        throw new Exception("Unknown node type " + node);
      }

    }

    // we should be at the expected location
    System.out.println("Final node: "+node);
    if ( !(node instanceof ScalarNode) )
      throw new Exception("Final node must be scalar: "+node);

    return this;
  }
}

class RetainAnchorGenerator implements AnchorGenerator{
  private int lastAnchorId;
  public RetainAnchorGenerator() {
    this.lastAnchorId = 0;
  }

  public String nextAnchor(Node node) {
    if ( node.getAnchor() != null )
      return node.getAnchor();

    this.lastAnchorId++;
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setMinimumIntegerDigits(3);
    format.setMaximumFractionDigits(0);// issue 172
    format.setGroupingUsed(false);
    String anchorId = format.format(this.lastAnchorId);
    return "id" + anchorId;
  }
}
