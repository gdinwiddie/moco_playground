package com.gdinwiddie.moco_playground;

import static com.github.dreamhead.moco.Moco.*;
import static com.github.dreamhead.moco.Runner.running;
import static com.github.dreamhead.moco.MocoJsonRunner.jsonHttpServer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.github.dreamhead.moco.*;
import com.github.dreamhead.moco.Runnable;
import com.google.gson.Gson;

public class ExploringMoco {

	private HttpServer server;

	@Before
	public void setUp() {
		server = httpServer(12345);
	}

	@Test
	/**
	 * from https://github.com/dreamhead/moco/blob/master/moco-doc/usage.md
	 * 
	 * @throws Exception
	 */
	public void should_respond_as_expected() throws Exception {
		server.response("foo");

		running(server, new Runnable() {
			public void run() throws IOException {
				Content content = Request.Get("http://localhost:12345").execute().returnContent();
				assertThat(content.asString(), is("foo"));
			}
		});
	}

	@Test
	public void get_versus_post() throws Exception {
		server.get(by(uri("/foo"))).response("Gotten");
		server.post(by(uri("/foo"))).response("Posted");

		running(server, new Runnable() {
			public void run() throws IOException {
				assertThat(Request.Get("http://localhost:12345/foo").execute().returnContent().asString(),
						is("Gotten"));
				assertThat(Request.Post("http://localhost:12345/foo").execute().returnContent().asString(),
						is("Posted"));
			}
		});
	}

	@Test
	public void json_file_config() throws Exception {

        server = jsonHttpServer(12345, file("bar.json"));

		running(server, new Runnable() {
			public void run() throws IOException {
				assertThat(Request.Get("http://localhost:12345/").execute().returnContent().asString(),
						is("bar"));
			}
		});
	}

	@Test 
	public void json_string_config() throws Exception {
		String configString = "[{ "
				//+ "\"request\" : {  \"method\" : \"post\", \"uri\" : \"\foo\" },"
				+ "\"response\" : { \"text\" : \"bar\" }"
				+ "}]";
		server = jsonHttpServer(12345, text(configString));

		running(server, new Runnable() {
			public void run() throws IOException {
				assertThat(Request.Post("http://localhost:12345/").execute().returnContent().asString(),
						is("bar"));
			}
		});
	}

	class Pojo {
		public int amount;
		public int getAmount() {
			return amount;
		}
		public String getCc_number() {
			return cc_number;
		}
		public String cc_number;
		public Pojo(Integer amount, String cc_number) {
			this.amount = amount;
			this.cc_number = cc_number;
		}
	}
	
	@Test 
	public void json_pojo_config() throws Exception {
		Pojo query = new Pojo(10, "ABC123");
		server.request(json(query)).response("bar");
		server.response("foo");

		running(server, new Runnable() {
			public void run() throws IOException {
				String json = new Gson().toJson(new Pojo(10, "ABC123"));
				System.out.println("JSON:"+json);
				assertThat(Request.Post("http://localhost:12345/").bodyString(json, null).execute().returnContent().asString(),
						is("bar"));
			}
		});
	}
	
	@Test 
	public void json_value_config() throws Exception {
		server.request(eq(jsonPath("cc_number"), "ABC123")).response("bar");
		server.response("foo");

		running(server, new Runnable() {
			public void run() throws IOException {
				String json = new Gson().toJson(new Pojo(10, "ABC123"));
				System.out.println("JSON:"+json);
				assertThat(Request.Post("http://localhost:12345/").bodyString(json, null).execute().returnContent().asString(),
						is("bar"));
			}
		});
	}
	
	@Test 
	public void json_cc_config() throws Exception {
        server = jsonHttpServer(12345, file("cc.json"));
		server.response("foo");

		running(server, new Runnable() {
			public void run() throws IOException {
				String goodCard = new Gson().toJson(new Pojo(11, "4111111111111111"));
				assertThat(Request.Post("http://localhost:12345/").bodyString(goodCard, null).execute().returnContent().asString(),
						is("success"));
				String insufficientCard = new Gson().toJson(new Pojo(9, "4444444444444448"));
				assertThat(Request.Post("http://localhost:12345/").bodyString(insufficientCard, null).execute().returnContent().asString(),
						is("processor failure"));
				String badCard = new Gson().toJson(new Pojo(8, "4222222222222220"));
				assertThat(Request.Post("http://localhost:12345/").bodyString(badCard, null).execute().returnContent().asString(),
						is("invalid card"));
			}
		});
	}
	
	@Test 
	public void json_cc_config_nesting() throws Exception {
        server = jsonHttpServer(12345, file("cc.json"));
		server.response("foo");

		running(server, new Runnable() {
			public void run() throws IOException {
				String goodCard = "{ \"amount\": 10, \"payment_info\": { \"cc_number\": \"4111111111111111\", \"CVV2\": \"123\", \"expiration\": \"09/24\" } }";
				assertThat(Request.Post("http://localhost:12345/").bodyString(goodCard, null).execute().returnContent().asString(),
						is("success"));
			}
		});
	}

}
