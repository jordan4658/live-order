package com.caipiao.live.order.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;


public class XmlObjMcUtil {

    private static final Logger logger = LoggerFactory.getLogger(XmlObjMcUtil.class);
    /**
     * @param obj
     * @param encoding
     * @return String
     * @throws JAXBException
     * @Title: ObjectToXml
     * @Description: TODO(javabean to xml)
     */
    public static String ObjectToXml(Object obj, String encoding) throws JAXBException {

        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);

        return writer.toString();

    }

    /**
     * @param xml
     * @param clazz
     * @return Object
     * @Title: converyToJavaBean
     * @Description: TODO(xml 转成 javabean)
     */
    @SuppressWarnings("unchecked")
    public static <T> T converyToJavaBean(String xml, Class<T> c) {
        T t = null;
        try {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            t = (T) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            logger.error("converyToJavaBean occur error.", e);
        }
        return t;
    }


    public static void main(String[] agrs) {
		
		/*RequestNetPayMent pay=new RequestNetPayMent();
		pay.setCurrency("CNY");
		String rs=null;
		try {
			rs=XmlObjMcUtil.ObjectToXml(pay,"UTF-8");
			System.out.println(rs);
		} catch (JAXBException e){
			//TODO Auto-generated catch bloc
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}*/
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<payment>" +
                "<tranName>payment</tranName>" +
                "<version>1.00</version>" +
                "<merCode>yunwei_pay_test</merCode>" +
                "<flowNo>569327</flowNo>" +
                "<orderNo>20170713133220DT50974</orderNo>" +
                "<orderDate>20170713133220</orderDate>" +
                "<ordAmt>10</ordAmt>" +
                "<perFee></perFee>" +
                "<currency>CNY</currency>" +
                "<payType>12</payType>" +
                "<paymentState>01</paymentState>" +
                "<orderDealTime></orderDealTime>" +
                "<workdate>20170713</workdate>" +
                "<clearDate>20170713</clearDate>" +
                "<validateField></validateField>" +
                "<sign>B052EFE68087ACD0A86041428527E990</sign>" +
                "<errorCode>错误：Tx0003</errorCode>" +
                "<errorMessage>中华人名共和国</errorMessage>" +
                "</payment>";

        //	ResponseNetPayMent pament=(ResponseNetPayMent) XmlObjMcUtil.converyToJavaBean(xml, ResponseNetPayMent.class);
        //	System.out.println("----ResponseNetPayMent----->"+pament.getOrderDate());
    }

}
