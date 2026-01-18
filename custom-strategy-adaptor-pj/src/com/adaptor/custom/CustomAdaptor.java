package com.adaptor.custom;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.beans.factory.InitializingBean;

public class CustomAdaptor implements MessageListener, InitializingBean {
	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(Message arg0) {
		// TODO Auto-generated method stub

	}
}
