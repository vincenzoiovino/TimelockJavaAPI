package org.zone.timelock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Timelock {
	public final static String timezone="Europe/Rome";
	public final static String timelock_zone_utc = " UTC+01:00";

	public static void Setup()
	{


	}

	/* for future use in which the URLs do not return JSON but plain binary contents
	private static String getUrlContents(String theUrl) throws IOException
	{
		StringBuilder content = new StringBuilder();
		try
		{
			URL url = new URL(theUrl); // creating a url object
			URLConnection urlConnection = url.openConnection(); // creating a urlconnection object

			// wrapping the urlconnection in a bufferedreader
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			// reading from the urlconnection using the bufferedreader
			while ((line = bufferedReader.readLine()) != null)
			{
				content.append(line + "\n");
			}
			bufferedReader.close();
		}
		catch(Exception e)
		{
			throw new IOException();
		}
		return content.toString();
	}
	 */	
	private static String getPublicKeyfromURL(long Round, String scheme) throws IOException // TODO: handle exceptions in a nice way
	{
		JSONObject jsonObj=null;
		JSONObject finalobj=null;
		try
		{
			int scheme_numeric;
			if (scheme.equals("secp256k1")) scheme_numeric=2;
			else scheme_numeric=-1;
			URL url = new URL("https://api.timelock.zone/tlcs/timelock/v1beta1/keypairs/round_and_scheme/"+Round+"/"+scheme_numeric);
			JSONParser parser = new JSONParser();
			jsonObj = (JSONObject) parser.parse(new InputStreamReader(url.openStream()));
			JSONArray jsonArray = (JSONArray) jsonObj.get("keypairs");
			finalobj= (JSONObject) jsonArray.get(0);
			String s=(String) finalobj.get("public_key_pem"); 
			/*
			// activate it for debug
			System.out.println(jsonObj.toJSONString());
			System.out.println(finalobj.toJSONString());
			Path logfile=Paths.get("log.txt");
		    Files.write(logfile, finalobj.toJSONString().getBytes());
		    Path logfile2=Paths.get("log2.txt");
		    Files.write(logfile2, (Round+"").getBytes());
			 */

			if (s.equals("")) throw new IOException();
			else return s;

		}
		catch(Exception e)
		{
			e.printStackTrace();
			/*
			// activate it for debug
			File file = new File("log.txt");
			PrintStream ps = new PrintStream(file);
			e.printStackTrace(ps);
			Path logfile=Paths.get("log2.txt");
		    Files.write(logfile, (Round+"").getBytes());
			 */
			throw new IOException();
		}

	}


	private static String getSecretKeyfromURL(long Round, String scheme) throws IOException
	{
		JSONObject jsonObj;
		JSONObject finalobj;
		try
		{
			int scheme_numeric;
			if (scheme.equals("secp256k1")) scheme_numeric=2;
			else scheme_numeric=-1;
			URL url = new URL("https://api.timelock.zone/tlcs/timelock/v1beta1/keypairs/round_and_scheme/"+Round+"/"+scheme_numeric); 
			JSONParser parser = new JSONParser();
			jsonObj = (JSONObject) parser.parse(new InputStreamReader(url.openStream()));
			JSONArray jsonArray = (JSONArray) jsonObj.get("keypairs");
			finalobj= (JSONObject) jsonArray.get(0);
			String s=(String) finalobj.get("private_key_pkcs8");

			if (s.equals("")) throw new IOException();
			else return s;

		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new IOException();
		}

	}



	// private static long DRAND_GENESIS_TIME=1677685200; genesis time for old chain
	private static long DRAND_GENESIS_TIME=1692803367;

	private static int DRAND_FREQUENCY=3;
	private static byte[] stripPEM(String pem) throws IOException {

		Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
		String encoded = parse.matcher(pem).replaceFirst("$1");
		return Base64.getMimeDecoder().decode(encoded);
	}


	public static long DayToRound(Date date){ // convert a Date in the first round of the day. For instance, 01/01/2023, 02:34:01 will be converted in the round corresponding to 01/01/2023, 00:00:00
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Timelock.timezone));

		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		Date newdate = cal.getTime();
		long t= ((newdate.getTime())/1000);
		return (t-Timelock.DRAND_GENESIS_TIME)/Timelock.DRAND_FREQUENCY;
	}

	public static long DateToRound(Date date){ // convert a Date in the first round of the hour. For instance, 01/01/2023, 02:34:01 will be converted in the round corresponding to 01/01/2023, 02:00:00
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Timelock.timezone));
		cal.setTime(date);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		Date newdate = cal.getTime();
		long t= ((newdate.getTime())/1000);
		return (t-Timelock.DRAND_GENESIS_TIME)/Timelock.DRAND_FREQUENCY;
	}

	public static long UnixTimeToRound(long ut){ // convert a Unix time (time in milliseconds since the Unix Epoch)
		long t= ut/1000;
		return (t-Timelock.DRAND_GENESIS_TIME)/Timelock.DRAND_FREQUENCY;
	}

	public static byte[] getPublicKeyFromRound(long Round, String Scheme) throws IOException {
		try{
			return stripPEM(getPublicKeyfromURL(Round,Scheme));

		} catch(IOException e) {
			e.printStackTrace();
			throw  new IOException();
		}

		/*        
		Examples of how pkpem should look like:
		String pkpem="-----BEGIN PUBLIC KEY-----\n" +
				"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEOErqrRCc3yBTCdQNfUQNM85JJHXOqYSH\n" +
				"ibnuF1AtHTgc1iOxS/OlGyVctEF+wJMLrvc/nrd2GhRYcqtsJu9Gfw==\n" +
				"-----END PUBLIC KEY-----\n";


		String pkpem="-----BEGIN PUBLIC KEY-----\n"
				+ "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE4ayNtqjWab3VdTiCozknOoZOETJoFWRU\n"
				+ "8BB8JdCsns3x5G9jsrkw4iJueGOwVQ+w1cNBqOlRWTVBkNK2Kjs9bQ==\n"
				+ "-----END PUBLIC KEY-----\n";
		return stripPEM(pkpem);
		 */

	}
	public static byte[] getSecretKeyFromRound(long Round, String Scheme) throws IOException {

		// TODO: retrieve sk for given round Round and given scheme Scheme
		// for the moment the sk is embedded
		try{
			return stripPEM(getSecretKeyfromURL(Round,Scheme));

		} catch(IOException e) {
			throw  new IOException();
		}
		/*
		 Examples of how skpem should look like:
		String skpem="-----BEGIN PRIVATE KEY-----\n" +
				"MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQg+uEhcA+bG/44RS/COUJa\n" +
				"bjwVrYcMKN8zby1LowdBvnihRANCAAQ4SuqtEJzfIFMJ1A19RA0zzkkkdc6phIeJ\n" +
				"ue4XUC0dOBzWI7FL86UbJVy0QX7Akwuu9z+et3YaFFhyq2wm70Z/\n" +
				"-----END PRIVATE KEY-----\n";
		String skpem="-----BEGIN PRIVATE KEY-----\n"
				+ "MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCCcj8jXC0N8VUW3GWHbv946\n"
				+ "P1n1MSnnuHL+oui/xp77xw==\n"
				+ "-----END PRIVATE KEY-----\n";

		return stripPEM(skpem);
		 */

	}
}
