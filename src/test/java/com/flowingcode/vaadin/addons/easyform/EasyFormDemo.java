package com.flowingcode.vaadin.addons.easyform;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Easy Form Add-on Demo")
@SuppressWarnings("serial")
@Route(value = "demo", layout = EasyFormDemoView.class)
public class EasyFormDemo extends Div {

  public EasyFormDemo() {
    add(new EasyFormAddon());
  }
}
