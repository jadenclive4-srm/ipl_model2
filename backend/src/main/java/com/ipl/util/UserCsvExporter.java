package com.ipl.util;

import com.ipl.model.mongo.UserMongo;
import com.ipl.repository.mongo.UserMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
public class UserCsvExporter implements CommandLineRunner {

    @Autowired
    private UserMongoRepository userMongoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "export-users".equals(args[0])) {
            String filePath = args.length > 1 ? args[1] : "users.csv";
            exportUsersToCsv(filePath);

            // Exit after export
            System.exit(0);
        }
    }

    public void exportUsersToCsv(String filePath) throws IOException {
        List<UserMongo> users = userMongoRepository.findAll();

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("username,email,fullName,role\n");

            // Write user data
            for (UserMongo user : users) {
                writer.write(user.getUsername() + ",");
                writer.write(user.getEmail() + ",");
                writer.write((user.getFullName() != null ? user.getFullName() : "") + ",");
                writer.write((user.getRole() != null ? user.getRole() : "USER") + "\n");
            }
        }

        System.out.println("Exported " + users.size() + " users to " + filePath);
    }

    public static void main(String[] args) {
        // Run as standalone application for export
        SpringApplication app = new SpringApplication(UserCsvExporter.class);
        ConfigurableApplicationContext context = app.run(args);
        context.close();
    }
}