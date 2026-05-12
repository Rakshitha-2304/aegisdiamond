package com.aegisdiamond.app.exception;

import com.aegisdiamond.auth.exception.InvalidCredentialsException;
import com.aegisdiamond.auth.exception.UserAlreadyExistsException;
import com.aegisdiamond.auth.exception.ValidationException;
import com.aegisdiamond.diamond.exception.DiamondNotFoundException;
import com.aegisdiamond.diamond.exception.InvalidCertificateException;
import com.aegisdiamond.shipping.exception.ShipmentNotFoundException;
import com.aegisdiamond.payment.exception.PaymentFailedException;
import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class GlobalGrpcExceptionHandler {

    @GrpcExceptionHandler(UserAlreadyExistsException.class)
    public Status handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        return Status.ALREADY_EXISTS.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(InvalidCredentialsException.class)
    public Status handleInvalidCredentialsException(InvalidCredentialsException e) {
        return Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(ValidationException.class)
    public Status handleValidationException(ValidationException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(DiamondNotFoundException.class)
    public Status handleDiamondNotFoundException(DiamondNotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(InvalidCertificateException.class)
    public Status handleInvalidCertificateException(InvalidCertificateException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(ShipmentNotFoundException.class)
    public Status handleShipmentNotFoundException(ShipmentNotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(PaymentFailedException.class)
    public Status handlePaymentFailedException(PaymentFailedException e) {
        return Status.ABORTED.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(Exception.class)
    public Status handleAnyException(Exception e) {
        return Status.INTERNAL.withDescription("An internal error occurred: " + e.getMessage()).withCause(e);
    }
}
