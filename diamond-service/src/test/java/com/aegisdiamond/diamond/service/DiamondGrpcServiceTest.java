package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.entity.Diamond;
import com.aegisdiamond.diamond.grpc.*;
import com.aegisdiamond.diamond.repository.DiamondRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiamondGrpcServiceTest {

    @Mock
    private DiamondRepository diamondRepository;

    @Mock
    private StreamObserver<DiamondResponse> responseObserver;

    @Mock
    private StreamObserver<CertificateResponse> certificateResponseObserver;

    @Mock
    private StreamObserver<SearchResponse> searchResponseObserver;

    private DiamondGrpcService diamondGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        diamondGrpcService = new DiamondGrpcService();
        setPrivateField(diamondGrpcService, "diamondRepository", diamondRepository);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void registerDiamond_Success() {
        DiamondRequest request = DiamondRequest.newBuilder()
                .setCut("Excellent")
                .setClarity("VVS1")
                .setColor("D")
                .setCarat(2.5)
                .setCertificateId(1001L)
                .setOwnerId(1L)
                .build();

        Diamond savedDiamond = new Diamond();
        savedDiamond.setId(1L);
        savedDiamond.setCut("Excellent");
        savedDiamond.setClarity("VVS1");
        savedDiamond.setColor("D");
        savedDiamond.setCarat(2.5);
        savedDiamond.setCertificateId(1001L);
        savedDiamond.setOwnerId(1L);
        savedDiamond.setStatus("REGISTERED");

        when(diamondRepository.save(any(Diamond.class))).thenReturn(savedDiamond);

        diamondGrpcService.registerDiamond(request, responseObserver);

        ArgumentCaptor<DiamondResponse> captor = ArgumentCaptor.forClass(DiamondResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        DiamondResponse response = captor.getValue();
        assertEquals(1L, response.getId());
        assertEquals("Excellent", response.getCut());
        assertEquals("VVS1", response.getClarity());
        assertEquals("D", response.getColor());
        assertEquals(2.5, response.getCarat(), 0.001);
        assertEquals("REGISTERED", response.getStatus());
    }

    @Test
    void updateDiamondDetails_Success() {
        Diamond existingDiamond = new Diamond();
        existingDiamond.setId(1L);
        existingDiamond.setCut("Good");
        existingDiamond.setClarity("VS1");
        existingDiamond.setColor("G");
        existingDiamond.setCarat(1.5);

        when(diamondRepository.findById(1L)).thenReturn(Optional.of(existingDiamond));
        when(diamondRepository.save(any(Diamond.class))).thenReturn(existingDiamond);

        DiamondRequest request = DiamondRequest.newBuilder()
                .setId(1L)
                .setCut("Excellent")
                .setClarity("VVS1")
                .setColor("D")
                .setCarat(2.0)
                .build();

        diamondGrpcService.updateDiamondDetails(request, responseObserver);

        ArgumentCaptor<DiamondResponse> captor = ArgumentCaptor.forClass(DiamondResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        assertEquals("Excellent", captor.getValue().getCut());
    }

    @Test
    void updateDiamondDetails_NotFound() {
        when(diamondRepository.findById(999L)).thenReturn(Optional.empty());

        DiamondRequest request = DiamondRequest.newBuilder()
                .setId(999L)
                .setCut("Excellent")
                .build();

        assertThrows(StatusRuntimeException.class, () ->
                diamondGrpcService.updateDiamondDetails(request, responseObserver));
    }

    @Test
    void verifyCertification_Valid() {
        Diamond diamond = new Diamond();
        diamond.setId(1L);
        diamond.setCertificateId(1001L);

        when(diamondRepository.findByCertificateId(1001L)).thenReturn(Optional.of(diamond));

        CertificateRequest request = CertificateRequest.newBuilder()
                .setCertificateId(1001L)
                .build();

        diamondGrpcService.verifyCertification(request, certificateResponseObserver);

        ArgumentCaptor<CertificateResponse> captor = ArgumentCaptor.forClass(CertificateResponse.class);
        verify(certificateResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getIsValid());
    }

    @Test
    void verifyCertification_Invalid() {
        when(diamondRepository.findByCertificateId(9999L)).thenReturn(Optional.empty());

        CertificateRequest request = CertificateRequest.newBuilder()
                .setCertificateId(9999L)
                .build();

        diamondGrpcService.verifyCertification(request, certificateResponseObserver);

        ArgumentCaptor<CertificateResponse> captor = ArgumentCaptor.forClass(CertificateResponse.class);
        verify(certificateResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getIsValid());
    }

    @Test
    void linkCertificate_Success() {
        Diamond diamond = new Diamond();
        diamond.setId(1L);
        diamond.setStatus("REGISTERED");

        when(diamondRepository.findById(1L)).thenReturn(Optional.of(diamond));
        when(diamondRepository.save(any(Diamond.class))).thenReturn(diamond);

        LinkCertificateRequest request = LinkCertificateRequest.newBuilder()
                .setDiamondId(1L)
                .setCertificateId(1001L)
                .build();

        diamondGrpcService.linkCertificate(request, responseObserver);

        ArgumentCaptor<DiamondResponse> captor = ArgumentCaptor.forClass(DiamondResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("CERTIFIED", captor.getValue().getStatus());
    }

    @Test
    void getDiamondById_Success() {
        Diamond diamond = new Diamond();
        diamond.setId(1L);
        diamond.setCut("Excellent");
        diamond.setCarat(2.0);

        when(diamondRepository.findById(1L)).thenReturn(Optional.of(diamond));

        DiamondIdRequest request = DiamondIdRequest.newBuilder().setId(1L).build();

        diamondGrpcService.getDiamondById(request, responseObserver);

        verify(responseObserver).onNext(any(DiamondResponse.class));
        verify(responseObserver).onCompleted();
    }

    @Test
    void searchDiamonds_ReturnsResults() {
        Diamond diamond = new Diamond();
        diamond.setId(1L);
        diamond.setCut("Excellent");

        when(diamondRepository.findByCutContainingOrClarityContainingOrColorContaining(anyString(), anyString(), anyString()))
                .thenReturn(java.util.List.of(diamond));

        SearchRequest request = SearchRequest.newBuilder().setQuery("Excellent").build();

        diamondGrpcService.searchDiamonds(request, searchResponseObserver);

        ArgumentCaptor<SearchResponse> captor = ArgumentCaptor.forClass(SearchResponse.class);
        verify(searchResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getDiamondsList().isEmpty());
    }
}
