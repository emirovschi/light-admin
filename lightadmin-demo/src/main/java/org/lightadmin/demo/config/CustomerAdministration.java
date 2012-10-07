package org.lightadmin.demo.config;

import com.google.common.base.Predicates;
import org.lightadmin.core.annotation.Administration;
import org.lightadmin.core.view.ScreenContext;
import org.lightadmin.core.view.support.filter.FilterBuilder;
import org.lightadmin.core.view.support.filter.Filters;
import org.lightadmin.core.view.support.fragment.Fragment;
import org.lightadmin.core.view.support.fragment.FragmentBuilder;
import org.lightadmin.core.view.support.scope.ScopeBuilder;
import org.lightadmin.core.view.support.scope.Scopes;
import org.lightadmin.demo.model.Customer;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.lightadmin.core.view.support.renderer.Renderers.select;
import static org.lightadmin.core.view.support.scope.ScopeUtils.*;

@Administration( Customer.class )
public class CustomerAdministration {

	public static void screenContext( ScreenContext screenContext ) {
		screenContext.screenName( "Customers Administration" ).menuName( "Customers" );
	}

	public static Fragment listView( final FragmentBuilder fragmentBuilder ) {
		return fragmentBuilder
			.field( "firstname" ).alias( "First Name")
			.field( "lastname" ).alias("Last Name")
			.field( "emailAddress" ).alias( "Email Address" ).build();
	}

	public static Scopes scopes( final ScopeBuilder scopeBuilder ) {
		return scopeBuilder
			.scope( "All", all() ).defaultScope()
			.scope( "Buyers", filter( Predicates.alwaysTrue() ) )
			.scope( "Sellers", specification( customerNameEqDave() ) ).build();
	}

	public static Filters filters( final FilterBuilder filterBuilder ) {
		return filterBuilder
			.field( "firstname" ).renderer( select( new String[] { "Yes", "No" } ))
			.field( "addresses" ).build();
	}

	public static Specification<Customer> customerNameEqDave() {
		return new Specification<Customer>() {
			@Override
			public Predicate toPredicate( final Root<Customer> root, final CriteriaQuery<?> query, final CriteriaBuilder cb ) {
				return cb.equal( root.get( "firstname" ), "Dave" );
			}
		};
	}
}