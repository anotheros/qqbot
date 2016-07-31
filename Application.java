package com.scienjus.smartqq;

import java.net.URL;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scienjus.smartqq.callback.MessageCallback;
import com.scienjus.smartqq.client.SmartQQClient;
import com.scienjus.smartqq.model.Category;
import com.scienjus.smartqq.model.DiscussMessage;
import com.scienjus.smartqq.model.Friend;
import com.scienjus.smartqq.model.Group;
import com.scienjus.smartqq.model.GroupMessage;
import com.scienjus.smartqq.model.Message;

import net.dongliu.requests.Client;
import net.dongliu.requests.Response;

/**
 * @author ScienJus
 * @date 2015/12/18.
 */
public class Application {

	static Queue<GroupMessage>	groupQueue	= new LinkedList<GroupMessage>();
	static Queue<Message>		userQueue	= new LinkedList<Message>();
	static Map<String, String>	cache		= new HashMap<String, String>();
	static List<String>			keywords	= new ArrayList<String>();
	static Map<Long, String>	groupMap	= new HashMap<Long, String>();
	static Map<Long, String>	friendMap	= new HashMap<Long, String>();
	public static final String	TURING_API	= "http://www.tuling123.com/openapi/api";
	public static final String	TURING_KEY	= "21741b883a494710bef1ee84c1a48245";
	public static final String	QQ_BOT_NAME	= "老张";

	public static void main(String[] args) {
		// 创建一个新对象时需要扫描二维码登录，并且传一个处理接收到消息的回调，如果你不需要接收消息，可以传null
		SmartQQClient client = new SmartQQClient(new MessageCallback() {
			@Override
			public void onMessage(Message message) {
				userQueue.add(message);
			}

			@Override
			public void onGroupMessage(GroupMessage message) {
				groupQueue.add(message);
				System.out.println(groupMap.get(message.getGroupId()) + ":" + message.getContent());
			}

			@Override
			public void onDiscussMessage(DiscussMessage message) {
				System.out.println(message.getContent());
			}
		});
		// 登录成功后便可以编写你自己的业务逻辑了
		List<Category> categories = client.getFriendListWithCategory();
		for (Category category : categories) {
			System.out.println(category.getName());
			for (Friend friend : category.getFriends()) {
				System.out.println(friend.getUserId() + ":" + friend.getNickname());
				friendMap.put(friend.getUserId(), friend.getNickname());
			}
		}
		List<Group> groups = client.getGroupList();

		for (Group group : groups) {
			groupMap.put(group.getId(), group.getName());
			System.out.println(group.getId() + ":" + group.getName());
		}
		// client.sendMessageToGroup(2951237343L,
		// System.currentTimeMillis()+"我是机器人");
		// client.sendMessageToGroup(3167544203L,System.currentTimeMillis()+
		// "我是机器人");

		Client Httpclient = Client.pooled().timeout(20000).maxPerRoute(5).maxTotal(10).build();
		Response<String> response = Httpclient.get("http://laozapp.com/1/tag/0/545").text();
		String tagJson = response.getBody();
		System.out.println(response.getBody());
		JSONArray jsonArray = JSON.parseArray(tagJson);
		int size = jsonArray.size();

		for (int i = 0; i < size; i++) {
			String tagName = jsonArray.getJSONObject(i).getString("tagName");
			if (tagName.length() > 1) {
				keywords.add(tagName);
			}
		}

		while (true) {
			Boolean groupb = groupQueue.isEmpty();
			if (!groupb) {
				GroupMessage groupMessage = groupQueue.poll();
				String content = groupMessage.getContent();
				Long id = groupMessage.getGroupId();
				String groupName = groupMap.get(id);
				System.out.println(groupName + "<--" + content);
				boolean showad = groupName.indexOf("微信公众号") > -1 && !groupName.equals("微信公众号运营交流") || groupName.equals("新浪云官方群");
				if (showad) {
					String ad = showAd(content);
					if (null != ad) {
						client.sendMessageToGroup(id, ad);
						System.out.println(groupName + "-->" + ad);
					}
				}
				Boolean xiaoqi = content.startsWith(QQ_BOT_NAME) || content.startsWith("@" + QQ_BOT_NAME);
				if (xiaoqi) {
					String messageString = sendMessage(content);
					if (null != messageString) {
						client.sendMessageToGroup(id, messageString);
						System.out.println(groupName + "-->" + messageString);
					}

					String chatString = chat(id + "", content);
					if (null != chatString && !"".equals(chatString.trim())) {
						client.sendMessageToGroup(id, chatString);
						System.out.println(groupName + "-->" + chatString);
					}

				}

			}
			Boolean userQueueb = userQueue.isEmpty();
			if (!userQueueb) {
				Message message = userQueue.poll();
				String content = message.getContent();
				Long id = message.getUserId();
				String friendName = friendMap.get(id);
				System.out.println(friendName + "<--" + content);
				String ad = showAd(content);
				if (null != ad) {
					client.sendMessageToFriend(id, ad);
					System.out.println(friendName + "-->" + ad);
				}
				String messageString = sendMessage(content);
				if (null != messageString) {
					client.sendMessageToFriend(id, messageString);
					System.out.println(friendName + "-->" + messageString);
				}

				String chatString = chat(id + "", content);
				if (null != chatString && !"".equals(chatString.trim())) {
					client.sendMessageToFriend(id, chatString);
					System.out.println(friendName + "-->" + chatString);
				} else {
					messageString = "你好，我不在电脑旁，有事微信";
					client.sendMessageToFriend(id, messageString);
					System.out.println(friendName + "-->" + messageString);
				}

			}
			if (groupb && userQueueb) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		// 使用后调用close方法关闭，你也可以使用try-with-resource创建该对象并自动关闭
		/*
		 * try { client.close(); } catch (IOException e) { e.printStackTrace();
		 * }
		 */
	}

	public static String sendMessage(String content) {
		Client Httpclient = Client.pooled().timeout(20000).maxPerRoute(5).maxTotal(10).build();
		System.out.println(keywords);
		for (String keyword : keywords) {
			if (content.indexOf(keyword) > -1 && !(content.indexOf("微信") > -1)) {
				String summarytagJson = cache.get(keyword);
				if (null == summarytagJson) {
					Response<String> responseSummary = Httpclient.get("http://laozapp.com/1/tagname/summary/" + keyword + "/0/100").text();
					summarytagJson = responseSummary.getBody();
					System.out.println(summarytagJson);
					cache.put(keyword, summarytagJson);
				}
				JSONArray summaryjsonArray = JSON.parseArray(summarytagJson);
				int summarysize = summaryjsonArray.size();
				double d = Math.random();
				int i = (int) (d * summarysize);
				String title = summaryjsonArray.getJSONObject(i).getString("title");
				String abstracts = summaryjsonArray.getJSONObject(i).getString("abstracts");
				String localUri = summaryjsonArray.getJSONObject(i).getString("localUri");
				String url = summaryjsonArray.getJSONObject(i).getString("url");
				return "您可能对这个感兴趣:" + title + url;
			}
		}
		return null;
	}

	public static String showAd(String content) {
		Boolean isWeixin = content.indexOf("微信公众号") > -1;
		if (isWeixin) {
			return "微信变现我自己搞了个程序你们看看吗？ http://ssp.laozapp.com (查看demo，ziyou_pub)";
		}
		Boolean isssp = content.indexOf("接单") > -1;
		if (isssp) {
			return "天天喊微信接单，我自己做了个自动接单的你们也不用  http://ssp.laozapp.com (查看demo，ziyou_pub)";
		}

		Boolean issae = content.indexOf("sae") > -1;
		if (issae) {
			return "sae 千秋万代一统江湖";

		}
		Boolean isyun = content.indexOf("云计算") > -1;
		if (isyun) {
			return "sae 千秋万代一统江湖，云计算还是sae的好";
		}
		Boolean iskeiche = content.indexOf("开车") > -1;
		if (iskeiche) {
			return "天天开车，不如看看我推荐的优质文章  http://laozapp.com";
		}
		return null;
	}

	public static String chat(final String userName, String msg) {
		if (msg.startsWith(QQ_BOT_NAME + " ")) {
			msg = msg.replace(QQ_BOT_NAME + " ", "");
		}
		if (msg.startsWith(QQ_BOT_NAME + "，")) {
			msg = msg.replace(QQ_BOT_NAME + "，", "");
		}
		if (msg.startsWith(QQ_BOT_NAME + ",")) {
			msg = msg.replace(QQ_BOT_NAME + ",", "");
		}
		if (msg.startsWith(QQ_BOT_NAME)) {
			msg = msg.replace(QQ_BOT_NAME, "");
		}
		Client Httpclient = Client.pooled().timeout(20000).maxPerRoute(5).maxTotal(10).build();

		try {

			final String body = "?key=" + URLEncoder.encode(TURING_KEY, "UTF-8") + "&info=" + URLEncoder.encode(msg, "UTF-8") + "&userid=" + URLEncoder.encode(userName, "UTF-8");
			Response<String> response = Httpclient.get(TURING_API + body).text();

			final JSONObject data = JSON.parseObject(response.getBody());
			final int code = data.getInteger("code");

			switch (code) {
			case 40001:
			case 40002:
			case 40007:

				return null;
			case 40004:
				return "聊累了，明天请早吧~";
			case 100000:
				return data.getString("text");
			case 200000:
				return data.getString("text") + " " + data.getString("url");
			case 302000:
				String ret302000 = data.getString("text") + " ";
				final JSONArray list302000 = data.getJSONArray("list");
				final StringBuilder builder302000 = new StringBuilder();
				for (int i = 0; i < list302000.size(); i++) {
					final JSONObject news = list302000.getJSONObject(i);
					builder302000.append(news.getString("article")).append(news.getString("detailurl")).append("\n\n");
				}

				return ret302000 + " " + builder302000.toString();
			case 308000:
				String ret308000 = data.getString("text") + " ";
				final JSONArray list308000 = data.getJSONArray("list");
				final StringBuilder builder308000 = new StringBuilder();
				for (int i = 0; i < list308000.size(); i++) {
					final JSONObject news = list308000.getJSONObject(i);
					builder308000.append(news.getString("name")).append(news.getString("detailurl")).append("\n\n");
				}

				return ret308000 + " " + builder308000.toString();
			default:

			}
		} catch (final Exception e) {

		}

		return null;
	}
}
