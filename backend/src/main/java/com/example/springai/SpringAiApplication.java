package com.example.springai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.example.springai.mapper") // 指定 Mapper 所在包
@SpringBootApplication
public class SpringAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiApplication.class, args);
		printStartupMessage();
	}

	/**
	 * 打印启动提示信息。
	 */
	private static void printStartupMessage() {
		System.out.println();
		System.out.println("  ____                                  _    ___ ");
		System.out.println(" |  _ \\ ___  ___ _   _ _ __ ___   ___  / \\  |_ _|");
		System.out.println(" | |_) / _ \\/ __| | | | '_ ` _ \\ / _ \\/ _ \\  | | ");
		System.out.println(" |  _ <  __/\\__ \\ |_| | | | | | |  __/ ___ \\ | | ");
		System.out.println(" |_| \\_\\___||___/\\__,_|_| |_| |_|\\___/_/   \\_\\___|");
		System.out.println();
	}

}
