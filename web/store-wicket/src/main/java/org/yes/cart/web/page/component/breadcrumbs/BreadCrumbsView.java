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

package org.yes.cart.web.page.component.breadcrumbs;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.yes.cart.constants.ServiceSpringKeys;
import org.yes.cart.domain.i18n.I18NModel;
import org.yes.cart.search.query.ProductSearchQueryBuilder;
import org.yes.cart.service.domain.ShopService;
import org.yes.cart.web.page.component.BaseComponent;
import org.yes.cart.web.service.wicketsupport.LinksSupport;
import org.yes.cart.web.support.constants.StorefrontServiceSpringKeys;
import org.yes.cart.web.support.constants.WebParametersKeys;

import java.util.List;

/**
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 7/30/11
 * Time: 10:42 AM
 */
public class BreadCrumbsView extends BaseComponent {

    private static final String BREADCRUMBS_LIST = "breadcrumbs";
    private static final String BREADCRUMBS_HOME_LINK = "homeLink";
    private static final String BREADCRUMBS_LINK = "link";
    private static final String BREADCRUMBS_REMOVE_LINK = "remLink";
    private static final String BREADCRUMBS_LINK_NAME = "linkName";

    @SpringBean(name = StorefrontServiceSpringKeys.BREAD_CRUMBS_BUILDER)
    private BreadCrumbsBuilder breadCrumbsBuilder;

    @SpringBean(name = ServiceSpringKeys.SHOP_SERVICE)
    private ShopService shopService;

    private final long customerShopId;
    private final long categoryId;

    /**
     * Build bread crumbs navigation view.
     *
     * @param id              component id
     * @param customerShopId  current shop id
     * @param categoryId      current category id
     */
    public BreadCrumbsView(final String id,
                           final long customerShopId,
                           final long categoryId) {

        super(id);

        this.categoryId = categoryId;
        this.customerShopId = customerShopId;

    }

    private List<Crumb> crumbs = null;

    /**
     * Get the crumbs
     * @return bread crumbs
     */
    public List<Crumb> getCrumbs() {
        if (crumbs == null) {

            final String pricePrefix = getLocalizer().getString(ProductSearchQueryBuilder.PRODUCT_PRICE, this);
            final String queryPrefix = getLocalizer().getString(WebParametersKeys.QUERY, this);
            final String tagPrefix = StringUtils.EMPTY;

            crumbs = breadCrumbsBuilder.getBreadCrumbs(getLocale().getLanguage(),
                    customerShopId, categoryId, getPage().getPageParameters(), shopService.getShopAllCategoriesIds(customerShopId),
                    pricePrefix, queryPrefix, tagPrefix);

        }
        return crumbs;
    }

    /** {@inheritDoc} */
    @Override
    protected void onBeforeRender() {

        add(
                new BookmarkablePageLink(BREADCRUMBS_HOME_LINK, Application.get().getHomePage())
        );
        add(
                new ListView<Crumb>(
                        BREADCRUMBS_LIST,
                        getCrumbs() ) {

                    @Override
                    protected void populateItem(final ListItem<Crumb> crumbListItem) {
                        final Crumb crumb = crumbListItem.getModelObject();
                        final I18NModel crumbModel;
                        if (ProductSearchQueryBuilder.PRODUCT_TAG_FIELD.equals(crumb.getKey())) {
                            String tag = crumb.getName();
                            try {
                                tag = getString(tag);
                            } catch (Exception exp) {
                                // catch for wicket unknown resource key exception
                            }
                            crumbModel = getI18NSupport().getFailoverModel(null, tag);
                        } else if (ProductSearchQueryBuilder.PRODUCT_INSTOCK_FIELD.equals(crumb.getKey())) {
                            String inStock = crumb.getName();
                            try {
                                inStock = getString(ProductSearchQueryBuilder.PRODUCT_INSTOCK_FIELD + inStock);
                            } catch (Exception exp) {
                                // catch for wicket unknown resource key exception
                            }
                            crumbModel = getI18NSupport().getFailoverModel(null, inStock);
                        } else {
                            crumbModel = getI18NSupport().getFailoverModel(crumb.getDisplayName(), crumb.getName());
                        }
                        final String selectedLocale = getLocale().getLanguage();

                        crumbListItem
                                .add(getPageLink(BREADCRUMBS_LINK, crumbModel.getValue(selectedLocale), new PageParameters(crumb.getCrumbLinkParameters()), true))
                                .add(getPageLink(BREADCRUMBS_REMOVE_LINK, crumbModel.getValue(selectedLocale), new PageParameters(crumb.getRemoveCrumbLinkParameters()), false)
                                );
                    }

                    private Link getPageLink(final String linkKey, final String linkName,
                                             final PageParameters parameters, final boolean addLabel) {

                        final LinksSupport links = getWicketSupportFacade().links();
                        final Link pageLink = links.newLink(linkKey, parameters);
                        if (addLabel) {
                            final Label label = new Label(BREADCRUMBS_LINK_NAME, linkName);
                            label.setEscapeModelStrings(false);
                            pageLink.add(label);
                        }
                        return pageLink;
                    }

                }
        );

        super.onBeforeRender();
    }

}
