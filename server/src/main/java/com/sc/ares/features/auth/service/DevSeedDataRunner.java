package com.sc.ares.features.auth.service;

import com.sc.ares.features.auth.entity.Role;
import com.sc.ares.features.auth.entity.UserRole;
import com.sc.ares.features.auth.repository.RoleRepository;
import com.sc.ares.features.auth.repository.UserRoleRepository;
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.entity.ClientStatus;
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.entity.CompanyStatus;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevSeedDataRunner implements ApplicationRunner {

    private static final String DEV_PASSWORD = "ares-dev";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedDataRunner(
        CompanyRepository companyRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        ClientRepository clientRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Company companyOne = ensureCompany("Ares Demo One");
        Company companyTwo = ensureCompany("Ares Demo Two");

        Map<String, Role> roles = Map.of(
            "super-admin", getRequiredRole("super-admin"),
            "company-admin", getRequiredRole("company-admin"),
            "user", getRequiredRole("user")
        );

        ensureUserWithRole(
            companyOne,
            "superadmin@local.ares",
            "Super",
            "Admin",
            roles.get("super-admin")
        );
        ensureUserWithRole(
            companyOne,
            "admin.one@local.ares",
            "Admin",
            "One",
            roles.get("company-admin")
        );
        ensureUserWithRole(
            companyOne,
            "user.one@local.ares",
            "User",
            "One",
            roles.get("user")
        );
        ensureUserWithRole(
            companyTwo,
            "admin.two@local.ares",
            "Admin",
            "Two",
            roles.get("company-admin")
        );
        ensureUserWithRole(
            companyTwo,
            "user.two@local.ares",
            "User",
            "Two",
            roles.get("user")
        );

        seedClients(companyOne, "alpha");
        seedClients(companyTwo, "beta");
    }

    private Company ensureCompany(String name) {
        return companyRepository.findByNameAndDeletedAtIsNull(name)
            .orElseGet(() -> companyRepository.save(Company.create(name, CompanyStatus.ACTIVE)));
    }

    private AppUser ensureUserWithRole(
        Company company,
        String email,
        String firstName,
        String lastName,
        Role role
    ) {
        AppUser user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
            .orElseGet(() -> userRepository.save(
                AppUser.create(
                    company,
                    email,
                    passwordEncoder.encode(DEV_PASSWORD),
                    firstName,
                    lastName,
                    UserStatus.ACTIVE
                )
            ));

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(UserRole.create(user, role));
        }

        return user;
    }

    private Role getRequiredRole(String code) {
        return roleRepository.findByCode(code)
            .orElseThrow(() -> new IllegalStateException("Required role not found: " + code));
    }

    private void seedClients(Company company, String companySlug) {
        List<DevClientSeed> clients = List.of(
            new DevClientSeed("Acme Retail", "100000001", "20000001", "office@acmeretail.test", "+381601000001", "Bulevar Oslobodjenja 10", "Novi Sad", "21000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Blue Harbor Logistics", "100000002", "20000002", "contact@blueharbor.test", "+381601000002", "Industrijska 14", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Cedar Books", "100000003", "20000003", "hello@cedarbooks.test", "+381601000003", "Kralja Petra 8", "Nis", "18000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Delta Trade", "100000004", "20000004", "sales@deltatrade.test", "+381601000004", "Cara Dusana 22", "Kragujevac", "34000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Evergreen Studio", "100000005", "20000005", "team@evergreenstudio.test", "+381601000005", "Takovska 5", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Fjord Systems", "100000006", "20000006", "support@fjordsystems.test", "+381601000006", "Maksima Gorkog 17", "Subotica", "24000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Granite Build", "100000007", "20000007", "office@granitebuild.test", "+381601000007", "Partizanska 31", "Novi Sad", "21000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Horizon Care", "100000008", "20000008", "info@horizoncare.test", "+381601000008", "Svetozara Markovica 44", "Cacak", "32000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Ivory Design", "100000009", "20000009", "studio@ivorydesign.test", "+381601000009", "Njegoseva 19", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Juniper Foods", "100000010", "20000010", "orders@juniperfoods.test", "+381601000010", "Vojvode Stepe 88", "Belgrade", "11010", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Keystone Labs", "100000011", "20000011", "contact@keystonelabs.test", "+381601000011", "Zeleznicka 12", "Sombor", "25000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Lighthouse Media", "100000012", "20000012", "hello@lighthousemedia.test", "+381601000012", "Balkanska 27", "Belgrade", "11000", "Serbia", ClientStatus.ACTIVE),
            new DevClientSeed("Monarch Travel", "100000013", "20000013", "desk@monarchtravel.test", "+381601000013", "Kosovska 3", "Nis", "18000", "Serbia", ClientStatus.INACTIVE),
            new DevClientSeed("Northwind Services", "100000014", "20000014", "office@northwindservices.test", "+381601000014", "Toplicina 15", "Kraljevo", "36000", "Serbia", ClientStatus.INACTIVE)
        );

        for (DevClientSeed seed : clients) {
            ensureClient(
                company,
                companySlug,
                seed.name(),
                seed.pib(),
                seed.mb(),
                seed.email(),
                seed.phone(),
                seed.address(),
                seed.city(),
                seed.postalCode(),
                seed.country(),
                seed.status()
            );
        }
    }

    private Client ensureClient(
        Company company,
        String companySlug,
        String name,
        String pib,
        String mb,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        ClientStatus status
    ) {
        String scopedName = "%s %s".formatted(name, companySlug.toUpperCase());

        return clientRepository.findByCompanyIdAndNameAndDeletedAtIsNull(company.getId(), scopedName)
            .orElseGet(() -> clientRepository.save(
                Client.create(
                    company,
                    scopedName,
                    pib,
                    mb,
                    email.replace("@", "+" + companySlug + "@"),
                    phone,
                    address,
                    city,
                    postalCode,
                    country,
                    status
                )
            ));
    }

    private record DevClientSeed(
        String name,
        String pib,
        String mb,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        ClientStatus status
    ) {
    }
}
