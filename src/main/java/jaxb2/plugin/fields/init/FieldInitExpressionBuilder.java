package jaxb2.plugin.fields.init;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.tools.xjc.model.CPluginCustomization;

import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static jaxb2.plugin.fields.init.InitBigDecimalFieldsPlugin.ATTR_EXECUTE_METHOD_QNAME;
import static jaxb2.plugin.fields.init.InitBigDecimalFieldsPlugin.ATTR_STATIC_VALUE_QNAME;

/**
 * Helper which builds JExpression for field instantiation
 */
public class FieldInitExpressionBuilder {
    /**
     * Inner tags and attributes that will be processed during plugin execution
     */
    private static final String EXECUTE_METHOD_NAME = "name";
    private static final String EXECUTE_METHOD_PARAM = "param";
    private static final String EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE = "type";

    private JExpression fieldInitExpression;

    public void apply(CPluginCustomization customization, JClass refBigDecimal, ErrorHandler errorHandler, Locator fieldLocator) throws SAXException {
        if (ATTR_STATIC_VALUE_QNAME.getLocalPart().equals(customization.element.getLocalName())) {
            applyStaticValue(customization, refBigDecimal);
        } else if (ATTR_EXECUTE_METHOD_QNAME.getLocalPart().equals(customization.element.getLocalName())) {
            applyMethodExecution(customization, refBigDecimal, errorHandler, fieldLocator);
        }
    }

    public JExpression getFieldInitExpression() {
        return this.fieldInitExpression;
    }

    private void applyStaticValue(CPluginCustomization customization, JClass refBigDecimal) {
        String tagText = customization.element.getTextContent();
        fieldInitExpression = refBigDecimal.staticRef(tagText);
    }

    private void applyMethodExecution(CPluginCustomization customization, JClass refBigDecimal, ErrorHandler errorHandler, Locator fieldLocator) throws SAXException {
        if (null == fieldInitExpression) {
            fatal("Tag <" + ATTR_EXECUTE_METHOD_QNAME.getLocalPart() + "> can't be first in the list of customizations. Hint: use <" + ATTR_STATIC_VALUE_QNAME.getLocalPart() + "> tag as first one.", errorHandler, fieldLocator);
        }

        //Process method name
        if (customization.element.getElementsByTagName(EXECUTE_METHOD_NAME).getLength() != 1) {
            fatal("Tag <" + ATTR_EXECUTE_METHOD_QNAME.getLocalPart() + "> must contain exactly one inner tag <" + EXECUTE_METHOD_NAME + ">...</" + EXECUTE_METHOD_NAME + ">", errorHandler, fieldLocator);
        }
        String methodName = customization.element.getElementsByTagName(EXECUTE_METHOD_NAME).item(0).getTextContent();
        JInvocation jInvocation = fieldInitExpression.invoke(methodName);

        //Process method params
        for (int i = 0; i < customization.element.getElementsByTagName(EXECUTE_METHOD_PARAM).getLength(); i++) {
            Node paramTag = customization.element.getElementsByTagName(EXECUTE_METHOD_PARAM).item(i);
            Node typeAttribute = paramTag.getAttributes().getNamedItem(EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE);
            if (null != typeAttribute) {
                String paramType = typeAttribute.getTextContent();
                String paramValue = paramTag.getTextContent();
                if (ParamType.BIG_DECIMAL.equals(paramType)) {
                    jInvocation = jInvocation.arg(refBigDecimal.staticRef(paramValue));
                } else if (ParamType.INT.equals(paramType)) {
                    jInvocation = jInvocation.arg(JExpr.lit(Integer.parseInt(paramValue)));
                } else {
                    fatal("Attribute **" + EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE + "** of tag <" + EXECUTE_METHOD_PARAM + "> contains unsupported value: " + paramType, errorHandler, fieldLocator);
                }
            } else {
                fatal("Tag <" + EXECUTE_METHOD_PARAM + "> must contain attribute **" + EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE + "**", errorHandler, fieldLocator);
            }
        }
        fieldInitExpression = jInvocation;
    }

    private void fatal(String msg, ErrorHandler errorHandler, Locator locator) throws SAXException {
        try {
            errorHandler.fatalError(new SAXParseException(msg, locator, null));
        } catch (SAXException ex) {
            //If exception was thrown - rethrow it. It's exactly what we want.
            throw ex;
        }
        //if method errorHandler.fatalError(..) didn't throw exception, - Do It!
        throw new SAXParseException(msg, locator, null);
    }
}