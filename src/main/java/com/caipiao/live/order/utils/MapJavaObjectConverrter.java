package com.caipiao.live.order.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class MapJavaObjectConverrter{
	private static final Logger logger = LoggerFactory.getLogger(MapJavaObjectConverrter.class);
	/**
	 * @Title: objectToMapString
	 * @Description: TODO(javaBean2MapString)
	 * @param obj
	 * @param
	 * @return Map<String,String>
	 * @throws IntrospectionException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public static Map<String,String> objectToMapString(Object obj, boolean isIgnorBlankOrNull){
		
		if(obj == null){  
            return null;  
        } 
		
		Map<String, String> map = new HashMap<>();
		try {
		        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());    
		        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();    
		        for (PropertyDescriptor property : propertyDescriptors) {    
		            String key = property.getName();    
		            if(key.compareToIgnoreCase("class") == 0) {   
		                continue;  
		            }  
		            Method getter = property.getReadMethod();  
		            Object value = getter!=null ? getter.invoke(obj): null;  
		            
		            if(value!=null){
		            	if(!StringUtils.isBlank(value.toString())) {
                            map.put(key, value.toString());
                        }
		            } 
		        }
			logger.info("map size"+map.size());
		} catch (Exception e){
			logger.error("objectToMapString occur error.", e);
		}
        return map;  
	}
	
	/**
	 * @Title: mapStringKeySortToLinkString
	 * @Description: TODO(排序非空参数生成签名原串)
	 * @param params
	 * @param isIgnorBlankOrNull
	 * @return String
	 */
	public static String mapStringKeySortToLinkString(Map<String,String> params,boolean isIgnorBlankOrNull){
		List<String> keys=new ArrayList<>(params.keySet());
		Collections.sort(keys);
		StringBuilder sb=new StringBuilder();
		int size=keys.size();
		for(int i=0;i<size;i++){
			String key=keys.get(i);
			String obj=params.get(key);
			if(!isIgnorBlankOrNull || !StringUtils.isBlank(obj)){
				sb.append(key).append("=").append(obj==null?"":obj);
				//最后一组参数，结尾不包括'&'
				if(i<size-1){
					sb.append("&");
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * @Title: mapStringKeySortToLinkString
	 * @Description: TODO(排序非空参数生成签名原串)
	 * @param params
	 * @param
	 * @return String
	 */
	public static String mapStringKeySortToLinkString(Map<String,String> params,String [] strArray){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<strArray.length;i++){
			String key=strArray[i];
			String obj=params.get(key);
	
				sb.append(key).append("=").append(obj==null?"":obj);
				//最后一组参数，结尾不包括'&'
				if(i<strArray.length-1){
					sb.append("&");
				}
		
		}
		return sb.toString();
	}

}
