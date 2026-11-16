package com.fincore.transaction.service;

import com.fincore.transaction.dto.PixEvent;
import com.fincore.transaction.dto.PixRequest;
import com.fincore.transaction.dto.PixResponse;
import com.fincore.transaction.client.AuthServiceClient;
import com.fincore.transaction.exception.BusinessException;
import com.fincore.transaction.kafka.PixEventProducer;
import com.fincore.transaction.model.PixStatus;
import com.fincore.transaction.model.PixTransaction;
import com.fincore.transaction.repository.PixTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixService - Testes Unitários")
class PixServiceTest {

    @Mock
    private PixTransactionRepository pixTransactionRepository;

    @Mock
    private PixEventProducer pixEventProducer;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private PixService pixService;

    private PixTransaction pixTransaction;
    private PixRequest pixRequest;

    @BeforeEach
    void setUp() {
        // Simula usuário autenticado no SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gustavo@fincore.com", null, List.of())
        );

        pixTransaction = PixTransaction.builder()
                .transactionId("txn-123")
                .senderAccountId(1L)
                .senderEmail("gustavo@fincore.com")
                .receiverKey("maria@fincore.com")
                .amount(new BigDecimal("100.00"))
                .description("Teste PIX")
                .status(PixStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        pixRequest = new PixRequest();
        pixRequest.setSenderAccountId(1L);
        pixRequest.setReceiverKey("maria@fincore.com");
        pixRequest.setAmount(new BigDecimal("100.00"));
        pixRequest.setDescription("Teste PIX");
        pixRequest.setTransactionPin("1234");
    }

    // ==================== INITIATE PIX ====================

    @Test
    @DisplayName("Deve iniciar PIX com sucesso")
    void initiatePix_success() {
        when(pixTransactionRepository.save(any(PixTransaction.class))).thenReturn(pixTransaction);
        doNothing().when(pixEventProducer).publishPixInitiated(any(PixEvent.class));

        PixResponse response = pixService.initiatePix(pixRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PixStatus.PENDING.name());
        verify(authServiceClient).validateTransactionPin("1234");
        verify(pixTransactionRepository).save(any(PixTransaction.class));
        verify(pixEventProducer).publishPixInitiated(any(PixEvent.class));
    }

    @Test
    @DisplayName("Não deve criar PIX quando o PIN de transação for rejeitado")
    void initiatePix_invalidPin_doesNotCreateTransaction() {
        doThrow(new BusinessException("PIN de transação inválido"))
                .when(authServiceClient).validateTransactionPin("1234");

        assertThatThrownBy(() -> pixService.initiatePix(pixRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PIN de transação inválido");

        verifyNoInteractions(pixTransactionRepository, pixEventProducer);
    }

    @Test
    @DisplayName("PIX deve ser criado com status PENDING")
    void initiatePix_shouldCreateWithPendingStatus() {
        when(pixTransactionRepository.save(any(PixTransaction.class))).thenReturn(pixTransaction);

        PixResponse response = pixService.initiatePix(pixRequest);

        assertThat(response.getStatus()).isEqualTo(PixStatus.PENDING.name());
    }

    @Test
    @DisplayName("Deve usar email do usuário autenticado como sender")
    void initiatePix_shouldUseSenderEmailFromSecurityContext() {
        when(pixTransactionRepository.save(any(PixTransaction.class))).thenAnswer(inv -> {
            PixTransaction txn = inv.getArgument(0);
            assertThat(txn.getSenderEmail()).isEqualTo("gustavo@fincore.com");
            return pixTransaction;
        });

        pixService.initiatePix(pixRequest);

        verify(pixTransactionRepository).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Deve publicar evento no Kafka após criar PIX")
    void initiatePix_shouldPublishKafkaEvent() {
        when(pixTransactionRepository.save(any(PixTransaction.class))).thenReturn(pixTransaction);

        pixService.initiatePix(pixRequest);

        verify(pixEventProducer, times(1)).publishPixInitiated(any(PixEvent.class));
    }

    @Test
    @DisplayName("Deve gerar transactionId único para cada PIX")
    void initiatePix_shouldGenerateUniqueTransactionId() {
        when(pixTransactionRepository.save(any(PixTransaction.class))).thenReturn(pixTransaction);

        pixService.initiatePix(pixRequest);

        verify(pixTransactionRepository).save(argThat(txn ->
                txn.getTransactionId() != null && !txn.getTransactionId().isEmpty()
        ));
    }

    // ==================== GET TRANSACTION ====================

    @Test
    @DisplayName("Deve buscar transação por ID com sucesso")
    void getTransaction_success() {
        pixTransaction.setReceiverAccountId(2L);
        when(pixTransactionRepository.findByTransactionId("txn-123"))
                .thenReturn(Optional.of(pixTransaction));

        PixResponse response = pixService.getTransaction("txn-123");

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("txn-123");
        assertThat(response.getReceiverAccountId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando transação não encontrada")
    void getTransaction_notFound_throwsException() {
        when(pixTransactionRepository.findByTransactionId("invalid-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pixService.getTransaction("invalid-id"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transação não encontrada");
    }

    // ==================== GET HISTORY ====================

    @Test
    @DisplayName("Deve retornar histórico de transações da conta")
    void getHistory_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<PixTransaction> page = new PageImpl<>(List.of(pixTransaction));

        when(pixTransactionRepository.findBySenderAccountIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(page);

        Page<PixResponse> result = pixService.getHistory(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSenderAccountId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve retornar página vazia quando não há histórico")
    void getHistory_empty() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(pixTransactionRepository.findBySenderAccountIdOrderByCreatedAtDesc(99L, pageable))
                .thenReturn(Page.empty());

        Page<PixResponse> result = pixService.getHistory(99L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}