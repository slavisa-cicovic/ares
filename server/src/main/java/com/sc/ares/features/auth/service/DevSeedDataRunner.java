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
import com.sc.ares.features.invoice.entity.Invoice;
import com.sc.ares.features.invoice.entity.InvoiceLine;
import com.sc.ares.features.invoice.entity.InvoiceStatus;
import com.sc.ares.features.invoice.repository.InvoiceRepository;
import com.sc.ares.features.item.entity.Item;
import com.sc.ares.features.item.entity.ItemStatus;
import com.sc.ares.features.item.repository.ItemRepository;
import com.sc.ares.features.payment.entity.Payment;
import com.sc.ares.features.payment.repository.PaymentRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.entity.UserStatus;
import com.sc.ares.features.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final ItemRepository itemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedDataRunner(
        CompanyRepository companyRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        ClientRepository clientRepository,
        ItemRepository itemRepository,
        InvoiceRepository invoiceRepository,
        PaymentRepository paymentRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.clientRepository = clientRepository;
        this.itemRepository = itemRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
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
        AppUser adminOne = ensureUserWithRole(
            companyOne,
            "admin.one@local.ares",
            "Admin",
            "One",
            roles.get("company-admin")
        );
        AppUser userOne = ensureUserWithRole(
            companyOne,
            "user.one@local.ares",
            "User",
            "One",
            roles.get("user")
        );
        AppUser adminTwo = ensureUserWithRole(
            companyTwo,
            "admin.two@local.ares",
            "Admin",
            "Two",
            roles.get("company-admin")
        );
        AppUser userTwo = ensureUserWithRole(
            companyTwo,
            "user.two@local.ares",
            "User",
            "Two",
            roles.get("user")
        );

        seedClients(companyOne, "alpha");
        seedClients(companyTwo, "beta");
        seedItems(companyOne, adminOne, userOne, "alpha");
        seedItems(companyTwo, adminTwo, userTwo, "beta");
        seedInvoices(companyOne, "alpha");
        seedInvoices(companyTwo, "beta");
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

    private void seedItems(
        Company company,
        AppUser adminUser,
        AppUser regularUser,
        String companySlug
    ) {
        List<DevItemSeed> items = List.of(
            new DevItemSeed("Bookkeeping Hour", "SRV-%s-001".formatted(companySlug.toUpperCase()), "Monthly bookkeeping work", "hour", "35.00", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Payroll Processing", "SRV-%s-002".formatted(companySlug.toUpperCase()), "Payroll preparation and reports", "service", "120.00", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("VAT Filing", "SRV-%s-003".formatted(companySlug.toUpperCase()), "VAT return filing", "service", "90.00", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Annual Statement", "SRV-%s-004".formatted(companySlug.toUpperCase()), "Year-end financial statement package", "service", "250.00", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Office Supplies", "GOODS-%s-001".formatted(companySlug.toUpperCase()), "General office supplies", "piece", "4.50", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Printer Toner", "GOODS-%s-002".formatted(companySlug.toUpperCase()), "Black toner cartridge", "piece", "78.00", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Archive Box", "GOODS-%s-003".formatted(companySlug.toUpperCase()), "Document archive storage box", "piece", "2.80", "20.00", ItemStatus.ACTIVE, regularUser),
            new DevItemSeed("Consulting Session", "SRV-%s-005".formatted(companySlug.toUpperCase()), "Financial consulting session", "hour", "55.00", "20.00", ItemStatus.ACTIVE, adminUser),
            new DevItemSeed("Tax Advisory", "SRV-%s-006".formatted(companySlug.toUpperCase()), "Tax advisory engagement", "service", "180.00", "20.00", ItemStatus.ACTIVE, adminUser),
            new DevItemSeed("Implementation Workshop", "SRV-%s-007".formatted(companySlug.toUpperCase()), "Onboarding workshop", "day", "320.00", "20.00", ItemStatus.ACTIVE, adminUser),
            new DevItemSeed("Cloud Subscription", "SUB-%s-001".formatted(companySlug.toUpperCase()), "Monthly software subscription", "month", "29.90", "20.00", ItemStatus.ACTIVE, adminUser),
            new DevItemSeed("Support Retainer", "SUB-%s-002".formatted(companySlug.toUpperCase()), "Monthly support retainer", "month", "150.00", "20.00", ItemStatus.ACTIVE, adminUser),
            new DevItemSeed("Legacy Maintenance", "SRV-%s-008".formatted(companySlug.toUpperCase()), "Legacy support package", "service", "95.00", "20.00", ItemStatus.INACTIVE, adminUser),
            new DevItemSeed("Dormant License", "SUB-%s-003".formatted(companySlug.toUpperCase()), "Inactive historical license", "month", "19.90", "20.00", ItemStatus.INACTIVE, adminUser)
        );

        for (DevItemSeed seed : items) {
            ensureItem(
                company,
                seed.createdByUser(),
                seed.name(),
                seed.code(),
                seed.description(),
                seed.unit(),
                seed.price(),
                seed.taxRate(),
                seed.status()
            );
        }
    }

    private Item ensureItem(
        Company company,
        AppUser createdByUser,
        String name,
        String code,
        String description,
        String unit,
        String price,
        String taxRate,
        ItemStatus status
    ) {
        return itemRepository.findByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNull(company.getId(), code)
            .orElseGet(() -> itemRepository.save(
                Item.create(
                    company,
                    createdByUser,
                    name,
                    code,
                    description,
                    unit,
                    new BigDecimal(price),
                    new BigDecimal(taxRate),
                    status
                )
            ));
    }

    private void seedInvoices(Company company, String companySlug) {
        List<Client> activeClients = clientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByNameAsc(company.getId())
            .stream()
            .filter(client -> client.getStatus() == ClientStatus.ACTIVE)
            .toList();
        List<Item> activeItems = itemRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByNameAsc(company.getId())
            .stream()
            .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
            .toList();

        if (activeClients.isEmpty() || activeItems.size() < 2) {
            return;
        }

        int sequenceYear = LocalDate.now().getYear();
        LocalDate baseIssueDate = LocalDate.now().minusDays(42);

        for (int index = 0; index < 14; index++) {
            Client client = activeClients.get(index % activeClients.size());
            Item firstItem = activeItems.get(index % activeItems.size());
            Item secondItem = activeItems.get((index + 3) % activeItems.size());
            String invoiceNumber = "%d-%04d".formatted(sequenceYear, index + 1);
            LocalDate issueDate = baseIssueDate.plusDays(index * 2L);
            LocalDate dueDate = issueDate.plusDays(15);
            boolean issued = index >= 4;
            BigDecimal paymentRatio = switch (index % 4) {
                case 1 -> new BigDecimal("0.50");
                case 2 -> BigDecimal.ONE;
                default -> BigDecimal.ZERO;
            };

            ensureInvoice(
                company,
                companySlug,
                client,
                firstItem,
                secondItem,
                invoiceNumber,
                sequenceYear,
                index + 1,
                issueDate,
                dueDate,
                issued,
                paymentRatio
            );
        }
    }

    private void ensureInvoice(
        Company company,
        String companySlug,
        Client client,
        Item firstItem,
        Item secondItem,
        String invoiceNumber,
        int sequenceYear,
        int sequenceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        boolean issued,
        BigDecimal paymentRatio
    ) {
        Invoice invoice = invoiceRepository.findByCompanyIdAndInvoiceNumberAndDeletedAtIsNull(company.getId(), invoiceNumber)
            .orElseGet(() -> invoiceRepository.save(createInvoice(
                company,
                companySlug,
                client,
                firstItem,
                secondItem,
                invoiceNumber,
                sequenceYear,
                sequenceNumber,
                issueDate,
                dueDate
            )));

        if (issued && invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.issue();
            invoice = invoiceRepository.save(invoice);
        }

        if (issued && paymentRatio.compareTo(BigDecimal.ZERO) > 0) {
            ensureInvoicePayment(invoice, paymentRatio);
        }
    }

    private Invoice createInvoice(
        Company company,
        String companySlug,
        Client client,
        Item firstItem,
        Item secondItem,
        String invoiceNumber,
        int sequenceYear,
        int sequenceNumber,
        LocalDate issueDate,
        LocalDate dueDate
    ) {
        BigDecimal firstQuantity = new BigDecimal("1.00");
        BigDecimal secondQuantity = new BigDecimal("2.00");

        InvoiceLine firstLine = createInvoiceLine(1, firstItem, firstQuantity);
        InvoiceLine secondLine = createInvoiceLine(2, secondItem, secondQuantity);

        BigDecimal subtotal = money(firstLine.getLineSubtotal().add(secondLine.getLineSubtotal()));
        BigDecimal taxTotal = money(firstLine.getLineTaxTotal().add(secondLine.getLineTaxTotal()));
        BigDecimal total = money(subtotal.add(taxTotal));

        Invoice invoice = Invoice.create(
            company,
            client,
            invoiceNumber,
            sequenceYear,
            sequenceNumber,
            issueDate,
            dueDate,
            "RSD",
            "DEV seed invoice for %s".formatted(companySlug.toUpperCase()),
            subtotal,
            taxTotal,
            total
        );
        invoice.replaceLines(List.of(firstLine, secondLine));

        return invoice;
    }

    private InvoiceLine createInvoiceLine(int lineNumber, Item item, BigDecimal quantity) {
        BigDecimal lineSubtotal = money(quantity.multiply(item.getPrice()));
        BigDecimal lineTaxTotal = money(
            lineSubtotal.multiply(item.getTaxRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
        );
        BigDecimal lineTotal = money(lineSubtotal.add(lineTaxTotal));

        return InvoiceLine.create(
            item,
            lineNumber,
            item.getName(),
            item.getCode(),
            item.getUnit(),
            quantity,
            item.getPrice(),
            item.getTaxRate(),
            lineSubtotal,
            lineTaxTotal,
            lineTotal
        );
    }

    private void ensureInvoicePayment(Invoice invoice, BigDecimal paymentRatio) {
        String note = "DEV seed payment %s".formatted(paymentRatio.stripTrailingZeros().toPlainString());
        if (paymentRepository.existsByInvoice_IdAndNote(invoice.getId(), note)) {
            return;
        }

        BigDecimal amount = money(invoice.getTotal().multiply(paymentRatio));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        paymentRepository.save(
            Payment.create(
                invoice,
                invoice.getIssueDate().plusDays(7),
                amount,
                note
            )
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
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

    private record DevItemSeed(
        String name,
        String code,
        String description,
        String unit,
        String price,
        String taxRate,
        ItemStatus status,
        AppUser createdByUser
    ) {
    }
}
