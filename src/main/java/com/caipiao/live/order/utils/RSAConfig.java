package com.caipiao.live.order.utils;

public class RSAConfig {

	/**
	 * 此文件编码格式为utf8无BOM格式 1）merchantPrivateKey，商户私钥 merchantPublicKey,商户公钥
	 * 商户需要按照《密钥对获取工具说明》操作并获取商户私钥，商户公钥。
	 * 2）代码中merchantPrivateKey、merchantPublicKey是测试商户号的商户私钥和商户公钥，请商家自行获取并且替换；
	 * 4）商户私钥在格式上要求换行，不要注释"-----BEGIN PRIVATE KEY-----"和"-----END PRIVATE KEY-----"
	 */
	public static final String merchantPrivateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAMcDlMT/00f647AJ"
			+ "SG/4Y0BAlG088keg9rzSztpqVkVD6Ad609rCF8V9R5dGr8K+pcLTT1LfTPx+Cop5"
			+ "lt0HdGwmSrGIUqUrgtpPVryaURmLIA3m9Vk/Af7AgxoxpYhBRo5nxpCJqs0BydM4"
			+ "Sck/JvgYGJuEExWgywxU8j7ibm2vAgMBAAECgYBKgS213qxjeyq4YZFL0eqeSE8I"
			+ "4lM1u64DnMwx+rNXdQetnS8o65boqXEe0ijEuIjn+iHtPYnd0PXzR3fSQZM0q7Oi"
			+ "BezLRU86OP0AQm/NQuAWoTeshKjQX/DLaCsun0adI/r419cb2EOBy6tZ7ZZ1hNCj"
			+ "/CR7fAJ5AODlJkx5OQJBAP1UFgNWa9pe/Q8+/3wqBKBVK9bj/pLrvplf+m67wXvA"
			+ "QS8vkYZkp42zYtaZv23nqv0gDDpwOlX6zqCZ2GU5CKMCQQDJHNyXSdyem576QVDh"
			+ "bPuTf1XIrLhl5Lg9vv686E6zsLjk/aipj/XRarGKYQZ6pp5Hky8xLH8SHXlSNBbo"
			+ "CVuFAkBF0TzX4qOK5Y78+rHS+ImZ3p9cdC2fNFWtU6RjjF+AybWtWYDT9z2ucfgV"
			+ "iP4XPjgD1ydHm9KYC62S2ZOoIhXdAkA7tegAuxtXtBi1cKMU6wieuFW96RouloPl"
			+ "QUncyJRlYXjj9DQZc/amIKlpznjf/YxM7/Q5A18O/9U/hNuwNOcJAkBWtnefm23C"
			+ "KWewEg+HSkKcYj8reBt/wYL1x67PocKPcUxERbuH3uG95u4zQnE4ougqRqgio5Nd" + "2MC76BPCQNnY";

	/**
	 * merchantPublicKey,商户公钥,由商户自行生成，并上传此公钥到支付商家后台，代码中不使用到此变量
	 */
	public static final String merchantPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDHA5TE/9NH+uOwCUhv+GNAQJRt"
			+ "PPJHoPa80s7aalZFQ+gHetPawhfFfUeXRq/CvqXC009S30z8fgqKeZbdB3RsJkqx"
			+ "iFKlK4LaT1a8mlEZiyAN5vVZPwH+wIMaMaWIQUaOZ8aQiarNAcnTOEnJPyb4GBib" + "hBMVoMsMVPI+4m5trwIDAQAB";

	/**
	 * 1)platformPublicKey，支付平台公钥，每个商家对应一个固定的支付公钥（不是使用工具生成的密钥merchantPublicKey，不要混淆），
	 * 不要注释"-----BEGIN PUBLIC KEY-----"和"-----END PUBLIC KEY-----"
	 * 2)demo提供的platformPublicKey是测试商户号的支付公钥，请自行复制对应商户号的支付公钥进行调整和替换。
	 */
	public static final String platformPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDX2+MjJByLLPj534fmS1Ou0faM"
			+ "gWttnv4DofcAnRzJrDRAmNwcMo74hII59cHbH7qBCi56inO+2puCe7xhT/Vn80Cq"
			+ "+S90FbGwHqYzFvR9lvHifapmOhc2UGsaGqjCuQnn+xzavAjPGFx8yLejGHt757Dj" + "o2c9HvKW3tGRqfsm9wIDAQAB";

	public static String getMerchantPrivateKey() {
		return merchantPrivateKey;
	}

	public static String getMerchantPublicKey() {
		return merchantPublicKey;
	}

	public static String getPlatformPublicKey() {
		return platformPublicKey;
	}
}
