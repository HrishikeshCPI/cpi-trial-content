<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method='xml' version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:template match="/">
    	<xsl:variable name="Lines" select="//Element[ApplicationDocument != ''] 
                                        |
                                         //Element/_ApplicationDocument[ApplicationDocument != '']"/>
    	<AppDocDeliveryItems>
        	<xsl:for-each select="$Lines">
    	       <AppDocDeliveryItem>
    	        <appDocNumber>
                    <xsl:value-of select="ApplicationDocument"/>
    		    </appDocNumber>
    		    <appDocItem>
                    <xsl:value-of select="ReferenceApplicationItem"/>
    		    </appDocItem>
    		    <appGUID>
                    <xsl:value-of select="ContractApplicationUUID"/>
    		    </appGUID>
    		    <appVersion>
                    <xsl:value-of select="ContractApplicationVersion"/>
    		    </appVersion>
    		    <LDCId>
                    <xsl:value-of select="LoadDataCaptureObjId"/>
    		    </LDCId>
    		    <contractNumber>
    	            <xsl:value-of select="TradingContractNumber"/>
    	        </contractNumber>
    		    <contractItemNumber>
    		        <xsl:value-of select="TradingContractItem"/>
    		    </contractItemNumber>
    		    <plantId>
                    <xsl:value-of select="Plant"/>
    		    </plantId>
    		    <grossQuantity>
    	            <xsl:value-of select="ACMDecimalGrossQuantity"/>
    	        </grossQuantity>
    	        <netQuantity>
    	            <xsl:value-of select="ACMNetQuantity"/>
    	        </netQuantity>
    	        <storageAgreement>
    	            <xsl:value-of select="_StorageDet/StorageAgreement"/>
    	        </storageAgreement>
    	        <storageAgreementItem>
    	            <xsl:value-of select="_StorageDet/StorageAgreementItem"/>
    	        </storageAgreementItem>
    	        <storageStartDate>
    	            <xsl:value-of select="_StorageDet/StorageStartDate"/>
    	        </storageStartDate>
    	        <storageEndDate>
    	            <xsl:value-of select="_StorageDet/StorageEndDate"/>
    	        </storageEndDate>
    	        <storageRatesLocked>
    	            <xsl:value-of select="_StorageDet/StorageRatesLockedValue"/>
    	        </storageRatesLocked>
    	        <storageFreeDays>
    	            <xsl:value-of select="_StorageDet/StorageFreeDaysValue"/>
    	        </storageFreeDays>
    	        <warehouseReceiptNumber>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptNo"/>
    	        </warehouseReceiptNumber>
    	        <warehouseReceiptTypeId>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptType"/>
    	        </warehouseReceiptTypeId>
    	        <warehouseStartDate>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptStartDate"/>
    	        </warehouseStartDate>
    	        <warehouseEndDate>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptEndDate"/>
    	        </warehouseEndDate>
    	        <warehouseReceiptQuantity>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptedQuantity"/>
    	        </warehouseReceiptQuantity>
    	        <openWarehouseRecieptQuantity>
    	            <xsl:value-of select="_WarehouseDet/WarehouseReceiptedOpenQty"/>
    	        </openWarehouseRecieptQuantity>
    	        <appDocStatusId>
    	            <xsl:value-of select="ACMAppDocStatus"/> 
    	        </appDocStatusId>
    	        <spotIndicatorId>
    	            <xsl:value-of select="ACMApplOvwSpotFlag"/>
    	        </spotIndicatorId>
    	        <loadLocationId>
    	            <xsl:value-of select="_OptionalityDet/ACMApplDocContrOptnLoadLoc"/>
    	        </loadLocationId>
    	        <dischargeLocationId>
    	            <xsl:value-of select="_OptionalityDet/ACMApplDocContrOptnDschrgdLoc"/>
    	        </dischargeLocationId>
    	        <modeOfTransportId>
    	            <xsl:value-of select="_OptionalityDet/ModeOfTransport"/>
    	        </modeOfTransportId>
    	        <meansOfTransportId>
    	            <xsl:value-of select="_OptionalityDet/MeansOfTransport"/>
    	        </meansOfTransportId>
    	        <sourceLocationId>
    	            <xsl:value-of select="_OptionalityDet/SourceLocation"/>
    	        </sourceLocationId>
    	        <deliveryStartDate>
    	            <xsl:value-of select="_OptionalityDet/DeliveryStartDate"/>
    	        </deliveryStartDate>
    	        <deliveryEndDate>
    	            <xsl:value-of select="_OptionalityDet/DeliveryEndDate"/>
    	        </deliveryEndDate>
    	        <cropSeasonId>
    	            <xsl:value-of select="_OptionalityDet/ACMContractOptionsCropSeasonID"/>
    	        </cropSeasonId>
    	        <LDCEventKey>
    	            <xsl:value-of select="ACMLoadDataCaptureEventKeyUUID"/>
    	        </LDCEventKey>
    	        <LDCEventDate>
    	            <xsl:value-of select="EventDate"/>
    	        </LDCEventDate>
    	        <scaleTicketNumber>
    	            <xsl:value-of select="ACMScaleTicketNumber"/>
    	        </scaleTicketNumber>
    	        <billOfLadingDate>
    	            <xsl:value-of select="BillOfLadingDate"/>
    	        </billOfLadingDate>
    	        <materialId>
    	            <xsl:value-of select="Material"/>
    	        </materialId>
    	        <commodityId>
    	            <xsl:value-of select="Commodity"/>
    	        </commodityId>
    	        <LDCConvertedQuantity>
    	            <xsl:value-of select="ACMLoadDataCaptureConvertQty"/>
    	        </LDCConvertedQuantity>
    	        <LDCUnconvertedQuantity>
    	            <xsl:value-of select="ACMLoadDataCapQuantity"/>
    	        </LDCUnconvertedQuantity>
    	        <LDCUnconvertedUoMId>
    	            <xsl:value-of select="UnitOfMeasure"/>
    	        </LDCUnconvertedUoMId>
    	        <overfillQuantity>
    	            <xsl:value-of select="ACMOverfillQuantity"/>
    	        </overfillQuantity>
    	        <underfillQuantity>
    	            <xsl:value-of select="ACMUnderFillQuantity"/>
    	        </underfillQuantity>
    	        <unitOfMeasureId>
    	            <xsl:value-of select="ACMAppDocBaseUoM"/>
    	        </unitOfMeasureId>
    	        <vendorId>
    	            <xsl:value-of select="Counterparty"/>
    	        </vendorId>
    	        <companyId>
    	            <xsl:value-of select="CompanyCode"/>
    	        </companyId>
    
                <xsl:choose>
                    <xsl:when test="_ToleranceEval[ContractApplicationUUID != '']">
                        <xsl:for-each select="_ToleranceEval">                	   
                            <tolerancesEvaluations>
                                <appGUID>
                                    <xsl:value-of select="ContractApplicationUUID"/>
                                </appGUID>
                                <appVersion>
                                    <xsl:value-of select="ContractApplicationVersion"/>
                                </appVersion>
                                <appDocNumber>
                                    <xsl:value-of select="ApplicationDocument"/>
                                </appDocNumber>
                                <appDocItem>
                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                </appDocItem>
                                <sequenceNo>
                                    <xsl:value-of select="ACMTolEvalTabSequenceNumber"/>
                                </sequenceNo>
                                <toleranceScheduleId>
                                    <xsl:value-of select="ACMTrdgContrToleranceSchedule"/>
                                </toleranceScheduleId>
                                <toleranceTypeId>
                                    <xsl:value-of select="ACMToleranceTypeID"/>
                                </toleranceTypeId>
                                <directionId>
                                    <xsl:value-of select="ACMUnderFillOverFillDirection"/>
                                </directionId>
                                <percentageFrom>
                                    <xsl:value-of select="ACMTolItemFromPercentageVal"/>
                                </percentageFrom>
                                <percentageTo>
                                <xsl:value-of select="ACMTolItemToPercentageVal"/>
                                </percentageTo>
                            </tolerancesEvaluations>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <tolerancesEvaluations>
                            <xsl:value-of select="_ToleranceEval"/>
                        </tolerancesEvaluations>
                    </xsl:otherwise>
                </xsl:choose>
    
                <xsl:choose>
                    <xsl:when test="_Quality[LDCEventKey != '']">
                        <xsl:for-each select="_Quality">
                            <qualities>
                                <LDCEventKey>
                                    <xsl:value-of select="LDCEventKey"/>
                                </LDCEventKey>
                                <version>
                                    <xsl:value-of select="QuantityRepositoryVersion"/>
                                </version>
                                <appDocNumber>
                                    <xsl:value-of select="ApplicationDocument"/>
                                </appDocNumber>
                                <appDocItem>
                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                </appDocItem>
                                <internalCharacterNumber>
                                    <xsl:value-of select="MsrgPtInternalCharacteristic"/>
                                </internalCharacterNumber>
                                <value>
                                    <xsl:value-of select="QtyRepositoryAttributeValue"/>
                                </value>
                                <valueUnitOfMeasureId>
                                    <xsl:value-of select="AppDocQualityAttributeUnit"/>
                                </valueUnitOfMeasureId>
                                <finalGradeId>
                                    <xsl:value-of select="ACMQualityIsFinalGrade"/>
                                </finalGradeId>
                            </qualities>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <qualities>
                            <xsl:value-of select="_Quality"/>
                        </qualities>
                    </xsl:otherwise>
                </xsl:choose>
    
                <xsl:choose>
                    <xsl:when test="_DPQSEvaluation[ContractApplicationUUID != '']">
                        <xsl:for-each select="_DPQSEvaluation">	
                            <DPQSEvaluations>
                                <appGUID>
                                    <xsl:value-of select="ContractApplicationUUID"/>
                                </appGUID>
                                <appVersion>
                                    <xsl:value-of select="ContractApplicationVersion"/>
                                </appVersion>
                                <appDocNumber>
                                    <xsl:value-of select="ApplicationDocument"/>
                                </appDocNumber>
                                <appDocItem>
                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                </appDocItem>
                                <sequenceNo>
                                    <xsl:value-of select="SequenceNumber"/>
                                </sequenceNo>
                                <internalCharacterNumber>
                                    <xsl:value-of select="DiscPremQtyIntChar"/>
                                </internalCharacterNumber>
                                <minimumValue>
                                    <xsl:value-of select="ACMMinimumDPQSValue"/>
                                </minimumValue>
                                <maximumValue>
                                    <xsl:value-of select="ACMMaximumDPQSValue"/>
                                </maximumValue>
                                <evaluatedQuantity>
                                    <xsl:value-of select="ACMEvaluatedQuantity"/>
                                </evaluatedQuantity>
                                <adjustedQuantity>
                                    <xsl:value-of select="ACMAdjustedQuantity"/>
                                </adjustedQuantity>
                                <unitOfMeasureId>
                                    <xsl:value-of select="UnitOfMeasure"/>
                                </unitOfMeasureId>	
    						
                                <xsl:choose>
                                    <xsl:when test="_AppDocDPQSUoM[ApplicationDocument != '']">
                                        <xsl:for-each select="_AppDocDPQSUoM">
                                            <DPQSUoMConversions>
                                                <appDocNumber>
                                                    <xsl:value-of select="ApplicationDocument"/>
                                                </appDocNumber>
                                                <appDocItem>
                                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                                </appDocItem>
                                                <sequenceNo>
                                                    <xsl:value-of select="SequenceNumber"/>
                                                </sequenceNo>
                                                <unitOfMeasureId>
                                                    <xsl:value-of select="PreferredUnitOfMeasure"/>
                                                </unitOfMeasureId>
                                                <evaluatedQuantity>
                                                    <xsl:value-of select="ACMEvaluatedQuantity"/>
                                                </evaluatedQuantity>
                                                <adjustedQuantity>
                                                    <xsl:value-of select="ACMAdjustedQuantity"/>
                                                </adjustedQuantity>
                                            </DPQSUoMConversions>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <DPQSUoMConversions>
                                            <xsl:value-of select="_AppDocDPQSUoM"/>
                                        </DPQSUoMConversions>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </DPQSEvaluations>				   				   
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <DPQSEvaluations>
                            <xsl:value-of select="_DPQSEvaluation"/>
                        </DPQSEvaluations>
                    </xsl:otherwise>
                </xsl:choose>
    			   
                <xsl:choose>
                    <xsl:when test="_AppDocUoM[ApplicationDocument != '']">
                        <xsl:for-each select="_AppDocUoM">	
                            <appDocUoMConversions>
                                <appDocNumber>
                                    <xsl:value-of select="ApplicationDocument"/>
                                </appDocNumber>	
                                <appDocItem>
                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                </appDocItem>
                                <unitOfMeasureId>
                                    <xsl:value-of select="PreferredUnitOfMeasure"/>
                                </unitOfMeasureId>
                                <LDCConvertedQuantity>
                                    <xsl:value-of select="ACMLoadDataCaptureConvertQty"/>
                                </LDCConvertedQuantity>
                                <grossQuantity>
                                    <xsl:value-of select="ACMDecimalGrossQuantity"/>
                                </grossQuantity>
                                <netQuantity>
                                    <xsl:value-of select="ACMNetQuantity"/>
                                </netQuantity>
                                <overfillQuantity>
                                    <xsl:value-of select="ACMOverfillQuantity"/>
                                </overfillQuantity>
                                <underfillQuantity>
                                    <xsl:value-of select="ACMUnderFillQuantity"/>
                                </underfillQuantity>					
                            </appDocUoMConversions>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <appDocUoMConversions>
                            <xsl:value-of select="_AppDocUoM"/>
                        </appDocUoMConversions>
                    </xsl:otherwise>
                </xsl:choose>
    
                <xsl:choose>
                    <xsl:when test="_WarehouseDet/_WarehouseUom[ApplicationDocument != '']">
                        <xsl:for-each select="_WarehouseDet/_WarehouseUom">	
                            <warehouseUoMConversions>
                                <appDocNumber>
                                    <xsl:value-of select="ApplicationDocument"/>
                                </appDocNumber>	
                                <appDocItem>
                                    <xsl:value-of select="ReferenceApplicationItem"/>
                                </appDocItem>
                                <unitOfMeasureId>
                                    <xsl:value-of select="PreferredUnitOfMeasure"/>
                                </unitOfMeasureId>
                                <appDocSide>
                                    <xsl:value-of select="ReferenceApplicationSide"/>
                                </appDocSide>
                                <appDocSubItem>
                                    <xsl:value-of select="ApplicationDocumentSubItem"/>
                                </appDocSubItem>
                                <warehouseReceiptQuantity>
                                    <xsl:value-of select="WarehouseReceiptedQuantity"/>
                                </warehouseReceiptQuantity>
                                <openWarehouseRecieptQuantity>
                                    <xsl:value-of select="WarehouseReceiptedOpenQty"/>
                                </openWarehouseRecieptQuantity>				
                            </warehouseUoMConversions>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <warehouseUoMConversions>
                            <xsl:value-of select="_WarehouseDet/_WarehouseUom"/>
                        </warehouseUoMConversions>
                    </xsl:otherwise>
                </xsl:choose>
    	   </AppDocDeliveryItem>
    	</xsl:for-each>    
    	</AppDocDeliveryItems>
	</xsl:template>
</xsl:stylesheet>