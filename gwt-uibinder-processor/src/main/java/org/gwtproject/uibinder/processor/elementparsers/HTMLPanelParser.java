/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtproject.uibinder.processor.elementparsers;

import org.gwtproject.uibinder.processor.UiBinderWriter;
import org.gwtproject.uibinder.processor.XMLElement;
import org.gwtproject.uibinder.processor.elementparsers.HtmlMessageInterpreter.PlaceholderInterpreterProvider;
import org.gwtproject.uibinder.processor.ext.UnableToCompleteException;
import org.gwtproject.uibinder.processor.messages.MessageWriter;
import org.gwtproject.uibinder.processor.messages.PlaceholderInterpreter;
import org.gwtproject.uibinder.processor.model.OwnerField;

import javax.lang.model.type.TypeMirror;

/**
 * Parses {@link com.google.gwt.user.client.ui.HTMLPanel} widgets.
 */
public class HTMLPanelParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, TypeMirror type,
      final UiBinderWriter writer) throws UnableToCompleteException {

    // Make sure that, if there is a UiField for this panel, it isn't
    // (provided = true), as that isn't supported.
    OwnerField uiField = writer.getOwnerClass().getUiField(fieldName);
    if (uiField != null && uiField.isProvided()) {
      writer.die("UiField %s for HTMLPanel cannot be provided.", fieldName);
    }

    /*
     * Gathers up elements that indicate nested widgets (but only those that are
     * not inside msg elements).
     */
    WidgetInterpreter widgetInterpreter = new WidgetInterpreter(fieldName,
        writer);

    /*
     * Handles non-widget elements like msg, and dom elements with ui:field
     * attributes. There may be widgets inside a msg, which is why the
     * construction in makeHtmlInterpreter is so complicated.
     */
    HtmlInterpreter htmlInterpreter = makeHtmlInterpreter(fieldName, writer);

    writer.beginAttachedSection(fieldName + ".getElement()");
    String html = elem.consumeInnerHtml(InterpreterPipe.newPipe(
        widgetInterpreter, htmlInterpreter));
    writer.endAttachedSection();

    /*
     * HTMLPanel has no no-arg ctor, so we have to generate our own, using the
     * element's innerHTML and perhaps its tag attribute. Do this in a way that
     * will not break subclasses if they happen to have the same constructor
     * signature (by passing in type).
     */
    String customTag = elem.consumeStringAttribute("tag", null);

    if (null == customTag) {
      writer.setFieldInitializerAsConstructor(fieldName,
          writer.declareTemplateCall(html, fieldName));
    } else {
      writer.setFieldInitializerAsConstructor(fieldName, customTag,
          writer.declareTemplateCall(html, fieldName));
    }
  }

  /**
   * Creates an HtmlInterpreter with our specialized placeholder interpreter, which will allow
   * widget instances to be declared inside of ui:msg elements.
   */
  private HtmlInterpreter makeHtmlInterpreter(final String fieldName,
      final UiBinderWriter uiWriter) {
    final String ancestorExpression = uiWriter.useLazyWidgetBuilders()
        ? fieldName : (fieldName + ".getElement()");

    PlaceholderInterpreterProvider placeholderInterpreterProvider =
        new PlaceholderInterpreterProvider() {
          public PlaceholderInterpreter get(MessageWriter message) {
            return new WidgetPlaceholderInterpreter(fieldName, uiWriter, message,
                ancestorExpression);
          }
        };

    HtmlInterpreter htmlInterpreter = new HtmlInterpreter(uiWriter,
        ancestorExpression, new HtmlMessageInterpreter(uiWriter,
        placeholderInterpreterProvider));

    return htmlInterpreter;
  }
}
