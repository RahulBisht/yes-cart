/*
 * Copyright 2009 Denys Pavlov, Igor Azarnyi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.service.dto.impl;

import com.inspiresoftware.lib.dto.geda.adapter.repository.AdaptersRepository;
import com.inspiresoftware.lib.dto.geda.assembler.Assembler;
import com.inspiresoftware.lib.dto.geda.assembler.DTOAssembler;
import org.yes.cart.constants.AttributeGroupNames;
import org.yes.cart.constants.AttributeNamesKeys;
import org.yes.cart.constants.Constants;
import org.yes.cart.dao.GenericDAO;
import org.yes.cart.domain.dto.AttrValueDTO;
import org.yes.cart.domain.dto.AttrValueShopDTO;
import org.yes.cart.domain.dto.AttributeDTO;
import org.yes.cart.domain.dto.ShopDTO;
import org.yes.cart.domain.dto.factory.DtoFactory;
import org.yes.cart.domain.dto.impl.AttrValueShopDTOImpl;
import org.yes.cart.domain.dto.impl.ShopDTOImpl;
import org.yes.cart.domain.entity.AttrValueShop;
import org.yes.cart.domain.entity.Attribute;
import org.yes.cart.domain.entity.Etype;
import org.yes.cart.domain.entity.Shop;
import org.yes.cart.domain.entity.impl.AttrValueEntityShop;
import org.yes.cart.exception.UnableToCreateInstanceException;
import org.yes.cart.exception.UnmappedInterfaceException;
import org.yes.cart.service.domain.*;
import org.yes.cart.service.dto.DtoAttributeService;
import org.yes.cart.service.dto.DtoShopService;
import org.yes.cart.utils.impl.AttrValueDTOComparatorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 09-May-2011
 * Time: 14:12:54
 */
public class DtoShopServiceImpl
        extends AbstractDtoServiceImpl<ShopDTO, ShopDTOImpl, Shop>
        implements DtoShopService {

    private final CustomerService customerService;

    private final Assembler attrValueAssembler;
    private final DtoAttributeService dtoAttributeService;
    private final GenericDAO<AttrValueEntityShop, Long> attrValueEntityShopDao;
    private final ImageService imageService;
    private final FileService fileService;
    private final SystemService systemService;


    public DtoShopServiceImpl(
            final ShopService shopService,
            final CustomerService customerService,
            final DtoFactory dtoFactory,
            final DtoAttributeService dtoAttributeService,
            final GenericDAO<AttrValueEntityShop, Long> attrValueEntityShopDao,
            final ImageService imageService,
            final FileService fileService,
            final AdaptersRepository adaptersRepository,
            final SystemService systemService) {
        super(dtoFactory, shopService, adaptersRepository);

        this.customerService = customerService;
        this.systemService = systemService;

        this.attrValueAssembler = DTOAssembler.newAssembler(
                dtoFactory.getImplClass(AttrValueShopDTO.class),
                shopService.getGenericDao().getEntityFactory().getImplClass(AttrValueShop.class)
        );
        this.dtoAttributeService = dtoAttributeService;
        this.attrValueEntityShopDao = attrValueEntityShopDao;
        this.imageService = imageService;
        this.fileService = fileService;


    }

    /** {@inheritDoc} */
    public List<ShopDTO> getAllTopLevel() throws UnmappedInterfaceException, UnableToCreateInstanceException {
        final List<Shop> shops = ((ShopService) service).getNonSubShops();
        final List<ShopDTO> dtos = new ArrayList<ShopDTO>();
        fillDTOs(shops, dtos);
        return dtos;
    }

    /** {@inheritDoc} */
    public List<ShopDTO> getAllSubs(final long masterId) throws UnmappedInterfaceException, UnableToCreateInstanceException {
        final List<Shop> shops = ((ShopService) service).getSubShopsByMaster(masterId);
        final List<ShopDTO> dtos = new ArrayList<ShopDTO>();
        fillDTOs(shops, dtos);
        return dtos;
    }

    /** {@inheritDoc} */
    public String getSupportedCurrencies(final long shopId) {
        return service.findById(shopId).getSupportedCurrencies();
    }

    /** {@inheritDoc} */
    public void updateSupportedCurrencies(final long shopId, final String currencies) {
        ((ShopService) service).updateAttributeValue(
                shopId,
                AttributeNamesKeys.Shop.SUPPORTED_CURRENCIES,
                currencies);
    }

    /** {@inheritDoc} */
    public List<String> getAllSupportedCurrenciesByShops() {
        return ((ShopService)service).findAllSupportedCurrenciesByShops();
    }

    /** {@inheritDoc} */
    public String getSupportedShippingCountries(final long shopId) {
        return service.findById(shopId).getSupportedShippingCountries();
    }

    /** {@inheritDoc} */
    public void updateSupportedShippingCountries(final long shopId, final String countries) {
        ((ShopService) service).updateAttributeValue(
                shopId,
                AttributeNamesKeys.Shop.SUPPORTED_COUNTRY_SHIP,
                countries);
    }

    /** {@inheritDoc} */
    public String getSupportedBillingCountries(final long shopId) {
        return service.findById(shopId).getSupportedBillingCountries();
    }

    /** {@inheritDoc} */
    public void updateSupportedBillingCountries(final long shopId, final String countries) {
        ((ShopService) service).updateAttributeValue(
                shopId,
                AttributeNamesKeys.Shop.SUPPORTED_COUNTRY_BILL,
                countries);
    }

    /** {@inheritDoc} */
    public String getSupportedLanguages(final long shopId) {
        return service.findById(shopId).getSupportedLanguages();
    }

    /** {@inheritDoc} */
    public void updateSupportedLanguages(final long shopId, final String languages) {
        ((ShopService) service).updateAttributeValue(
                shopId,
                AttributeNamesKeys.Shop.SUPPORTED_LANGUAGES,
                languages);
    }

    /** {@inheritDoc} */
    public ShopDTO getShopDtoByDomainName(final String serverDomainName) {
        final Shop shop =((ShopService)service).getShopByDomainName(serverDomainName);
        final ShopDTO dto = (ShopDTO) dtoFactory.getByIface(getDtoIFace());
        getAssembler().assembleDto(dto, shop, getAdaptersRepository(), getAssemblerDtoFactory());
        return dto;
    }

    /** {@inheritDoc} */
    public ShopDTO updateDisabledFlag(final long shopId, final boolean disabled) {
        final Shop shop = service.findById(shopId);
        shop.setDisabled(disabled);
        service.update(shop);
        final ShopDTO dto = (ShopDTO) dtoFactory.getByIface(getDtoIFace());
        getAssembler().assembleDto(dto, shop, getAdaptersRepository(), getAssemblerDtoFactory());
        return dto;
    }

    /** {@inheritDoc} */
    public Class<ShopDTO> getDtoIFace() {
        return ShopDTO.class;
    }

    /** {@inheritDoc} */
    public Class<ShopDTOImpl> getDtoImpl() {
        return ShopDTOImpl.class;
    }

    /** {@inheritDoc} */
    public Class<Shop> getEntityIFace() {
        return Shop.class;
    }

    /** {@inheritDoc}*/
    public List<? extends AttrValueDTO> getEntityAttributes(final long entityPk) throws UnmappedInterfaceException, UnableToCreateInstanceException {
        final List<AttrValueShopDTO> result = new ArrayList<AttrValueShopDTO>(getById(entityPk).getAttributes());
        final List<AttributeDTO> availableAttributeDTOs = dtoAttributeService.findAvailableAttributes(
                AttributeGroupNames.SHOP,
                getCodes(result));
        for (AttributeDTO attributeDTO : availableAttributeDTOs) {
            AttrValueShopDTO attrValueShopDTO = getAssemblerDtoFactory().getByIface(AttrValueShopDTO.class);
            attrValueShopDTO.setAttributeDTO(attributeDTO);
            attrValueShopDTO.setShopId(entityPk);
            result.add(attrValueShopDTO);
        }
        Collections.sort(result, new AttrValueDTOComparatorImpl());
        return result;
    }

    /** {@inheritDoc}*/
    public AttrValueDTO updateEntityAttributeValue(final AttrValueDTO attrValueDTO) {
        final AttrValueEntityShop valueEntityShop = attrValueEntityShopDao.findById(attrValueDTO.getAttrvalueId());
        attrValueAssembler.assembleEntity(attrValueDTO, valueEntityShop, getAdaptersRepository(), dtoFactory);
        attrValueEntityShopDao.update(valueEntityShop);
        return attrValueDTO;

    }

    /** {@inheritDoc}*/
    public AttrValueDTO createEntityAttributeValue(final AttrValueDTO attrValueDTO) {

        final Attribute atr = ((GenericService<Attribute>) dtoAttributeService.getService()).findById(attrValueDTO.getAttributeDTO().getAttributeId());
        final boolean multivalue = atr.isAllowduplicate();
        final Shop shop = service.findById(((AttrValueShopDTO) attrValueDTO).getShopId());
        if (!multivalue) {
            for (final AttrValueShop avp : shop.getAttributes()) {
                if (avp.getAttributeCode().equals(atr.getCode())) {
                    // this is a duplicate, so need to update
                    attrValueDTO.setAttrvalueId(avp.getAttrvalueId());
                    return updateEntityAttributeValue(attrValueDTO);
                }
            }
        }

        AttrValueShop valueEntityShop = getPersistenceEntityFactory().getByIface(AttrValueShop.class);
        attrValueAssembler.assembleEntity(attrValueDTO, valueEntityShop, getAdaptersRepository(), dtoFactory);
        valueEntityShop.setAttributeCode(atr.getCode());
        valueEntityShop.setShop(shop);
        valueEntityShop = attrValueEntityShopDao.create((AttrValueEntityShop) valueEntityShop);
        attrValueDTO.setAttrvalueId(valueEntityShop.getAttrvalueId());
        return attrValueDTO;


    }

    /** {@inheritDoc}*/
    public long deleteAttributeValue(final long attributeValuePk)
            throws UnmappedInterfaceException, UnableToCreateInstanceException{
        final AttrValueEntityShop valueEntityShop = attrValueEntityShopDao.findById(attributeValuePk);
        final AttributeDTO attributeDTO = dtoAttributeService.findByAttributeCode(valueEntityShop.getAttributeCode());
        if (Etype.IMAGE_BUSINESS_TYPE.equals(attributeDTO.getEtypeName())) {
            imageService.deleteImage(valueEntityShop.getVal(),
                    Constants.SHOP_IMAGE_REPOSITORY_URL_PATTERN, systemService.getImageRepositoryDirectory());
        } else if (Etype.FILE_BUSINESS_TYPE.equals(attributeDTO.getEtypeName())) {
            fileService.deleteFile(valueEntityShop.getVal(),
                    Constants.SHOP_FILE_REPOSITORY_URL_PATTERN, systemService.getFileRepositoryDirectory());
        } else if (Etype.SYSFILE_BUSINESS_TYPE.equals(attributeDTO.getEtypeName())) {
            fileService.deleteFile(valueEntityShop.getVal(),
                    Constants.SHOP_SYSFILE_REPOSITORY_URL_PATTERN, systemService.getSystemFileRepositoryDirectory());
        }
        attrValueEntityShopDao.delete(valueEntityShop);
        return valueEntityShop.getShop().getShopId();
    }

    /**
     * {@inheritDoc}
     */
    public AttrValueDTO createAndBindAttrVal(final long entityPk, final String attrName, final String attrValue)
            throws UnmappedInterfaceException, UnableToCreateInstanceException {
        throw new UnmappedInterfaceException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public AttrValueDTO getNewAttribute(final long entityPk) throws UnableToCreateInstanceException, UnmappedInterfaceException {
        final AttrValueShopDTO dto = new AttrValueShopDTOImpl();
        dto.setShopId(entityPk);
        return dto;
    }

}
