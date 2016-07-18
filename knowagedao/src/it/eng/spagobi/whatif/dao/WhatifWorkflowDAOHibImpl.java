/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.whatif.dao;

import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.commons.dao.AbstractHibernateDAO;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.profiling.bean.SbiUser;
import it.eng.spagobi.profiling.dao.ISbiUserDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.whatif.metadata.SbiWhatifWorkflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class WhatifWorkflowDAOHibImpl extends AbstractHibernateDAO implements IWhatifWorkflowDAO {

	private static transient Logger logger = Logger.getLogger(WhatifWorkflowDAOHibImpl.class);
	private static final String STATE_DONE = "done";
	private static final String STATE_NOT_STARTED_YET = "notstartedyet";
	private static final String STATE_INPROGRESS = "inprogress";

	@Override
	public void createNewWorkflow(List<SbiWhatifWorkflow> newWorkflow) {
		logger.debug("IN");
		Session aSession = null;
		Transaction tx = null;

		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			for (int i = 0; i < newWorkflow.size(); i++)
				aSession.save(newWorkflow.get(i));

			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			logger.error("Exception creating workflow", he);
			throw new SpagoBIRuntimeException("Exception creating workflow", he);
		} finally {
			logger.debug("Workflow created correctly");
			if (aSession != null) {
				if (aSession.isOpen())
					aSession.close();
			}
		}
	}

	@Override
	public SbiWhatifWorkflow loadUsersWorkflow() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getActiveUserIdByModel(int modelId){
		logger.debug("IN");
		logger.debug("loading the active user for the model "+modelId);
		try {
			Criteria criteria = getSession().createCriteria(SbiWhatifWorkflow.class);
			Criterion rest1 = Restrictions.eq("modelId", modelId);
			Criterion rest2 = Restrictions.eq("state", STATE_INPROGRESS);
			criteria.add(Restrictions.and(rest1, rest2));

			Object wf = criteria.uniqueResult();
			if(wf==null){
				return null;
			}else{
				SbiWhatifWorkflow el = (SbiWhatifWorkflow) wf;
				int userId = el.getUserId();
				return getUserName(userId);
			}
			
		} catch (Exception he) {
			logger.error("Exception loading the active user in the worflow", he);
			throw new SpagoBIRuntimeException("Exception loading the active user in the worflow", he);
		} finally{
			logger.debug("OUT");
		}
	}

	private String getUserName(int userId) throws EMFUserError{
		ISbiUserDAO userdao = DAOFactory.getSbiUserDAO();
		SbiUser user = userdao.loadSbiUserById(userId);
		return user.getUserId();
	}
	
	public List<SbiWhatifWorkflow> getWorkflowByModel(int modelId) {
		logger.debug("IN");
		List<SbiWhatifWorkflow> list;
		try {

			Criteria criteria = getSession().createCriteria(SbiWhatifWorkflow.class);
			Criterion rest1 = Restrictions.eq("modelId", modelId);
			criteria.add(rest1);
			criteria.addOrder(Order.asc("sequcence"));
			return criteria.list();

		} catch (HibernateException he) {
			logger.error("Error loading workflow for model"+ modelId, he);
			throw new SpagoBIRuntimeException("Error loading workflow for model"+ modelId, he);
		} 
		
	}
	
	@Override
	public List<Integer> getWorkflowUsersOfModel(int modelId) {
		logger.debug("IN");
		List list;
		List<Integer> resultArray = new ArrayList<>();

		try {


			Criteria criteria = getSession().createCriteria(SbiWhatifWorkflow.class);
			Criterion rest1 = Restrictions.eq("modelId", modelId);
			criteria.add(rest1);
			criteria.addOrder(Order.asc("sequcence"));
			list = criteria.list();
			Iterator it = list.iterator();

			while (it.hasNext()) {
				SbiWhatifWorkflow el = (SbiWhatifWorkflow) it.next();
				resultArray.add(el.getUserId());
			}

		} catch (HibernateException he) {
			logger.error("Exception loading workflow users", he);
			throw new SpagoBIRuntimeException("Exception loading workflow users", he);
		} 
		return resultArray;
	}

	@Override
	public int isWorkflowStarted(int modelId) {
		logger.debug("IN");

		try {

			Criteria criteria = getSession().createCriteria(SbiWhatifWorkflow.class);
			Criterion rest1 = Restrictions.eq("modelId", modelId);
			Criterion rest2 = Restrictions.eq("state", STATE_INPROGRESS);
			criteria.add(Restrictions.and(rest1, rest2));

			List list = criteria.list();
			if (list.size() > 1) {
				return -1;
			} else if (list.size() < 1) {
				return 0;
			} else {
				SbiWhatifWorkflow el = (SbiWhatifWorkflow) list.get(0);
				return el.getUserId();
			}
		} catch (HibernateException he) {
			logger.error("Exception loading workflow users", he);
			return 0;
		}


	}

	@Override
	public void startWorkflow(int modelId) {
		logger.debug("IN");
		Session aSession = null;
		Transaction tx = null;

		try {
			aSession = getSession();
			tx = aSession.beginTransaction();

			Query q = aSession.createSQLQuery("UPDATE SBI_WHATIF_WORKFLOW SET state = :stateValue WHERE model_id = :mId AND sequence = 0");
			q.setParameter("stateValue", STATE_INPROGRESS);
			q.setParameter("mId", modelId);
			q.executeUpdate();
			tx.commit();
		} catch (HibernateException he) {
			logger.error(he.getMessage(), he);
			if (tx != null)
				tx.rollback();
		} finally {
			if (aSession != null) {
				if (aSession.isOpen())
					aSession.close();
			}
		}

	}

	@Override
	public void updateWorkflow(List<SbiWhatifWorkflow> workflow) {
		logger.debug("IN");
		Session aSession = null;
		Transaction tx = null;
		int mId = workflow.get(0).getModelId();
		List<Integer> existing = getWorkflowUsersOfModel(mId);
		List<Integer> toRemove = toRemove(workflow, existing);
		List<Integer> toAdd = toAdd(workflow, existing);
		List<SbiWhatifWorkflow> wfToAdd = new ArrayList<>();
		if (toRemove.size() > 0)
			removeUsersForUpdate(toRemove, mId);

		try {
			aSession = getSession();
			tx = aSession.beginTransaction();

			for (int i = 0; i < workflow.size(); i++) {
				if (!toAdd.contains(workflow.get(i).getUserId())) {
					Query q = aSession.createSQLQuery("UPDATE SBI_WHATIF_WORKFLOW SET sequence = :seq WHERE model_id = :mId AND user_id = :uId");
					q.setParameter("seq", i);
					q.setParameter("mId", workflow.get(i).getModelId());
					q.setParameter("uId", workflow.get(i).getUserId());
					q.executeUpdate();
				} else {
					wfToAdd.add(workflow.get(i));
				}

			}
			tx.commit();
		} catch (HibernateException he) {
			logger.error(he.getMessage(), he);
			if (tx != null)
				tx.rollback();
		} finally {
			if (aSession != null) {
				if (aSession.isOpen())
					aSession.close();
			}

			if (!wfToAdd.isEmpty())
				createNewWorkflow(wfToAdd);
		}
	}

	public void removeUsersForUpdate(List<Integer> userIds, int modelId) {
		logger.debug("IN");
		Session aSession = null;
		Transaction tx = null;

		try {
			aSession = getSession();
			tx = aSession.beginTransaction();

			for (int i = 0; i < userIds.size(); i++) {
				Query q = aSession.createQuery("DELETE SbiWhatifWorkflow WHERE model_id= :mId AND user_id = :uId");
				q.setParameter("mId", modelId);
				q.setParameter("uId", userIds.get(i));

				q.executeUpdate();
			}

			tx.commit();
		} catch (HibernateException he) {
			logger.error(he.getMessage(), he);
			if (tx != null)
				tx.rollback();
		} finally {
			if (aSession != null) {
				if (aSession.isOpen())
					aSession.close();
			}
		}
	}

	private List<Integer> toRemove(List<SbiWhatifWorkflow> workflow, List<Integer> existing) {
		List<Integer> wfUserIds = userIdsFromWorkflow(workflow);
		List<Integer> result = new ArrayList<>();

		for (int i = 0; i < existing.size(); i++) {
			int curId = existing.get(i);

			if (!wfUserIds.contains(curId))
				result.add(curId);
		}

		return result;
	}

	private List<Integer> toAdd(List<SbiWhatifWorkflow> workflow, List<Integer> existing) {
		List<Integer> result = new ArrayList<>();

		for (int i = 0; i < workflow.size(); i++) {
			int curId = workflow.get(i).getUserId();

			if (!existing.contains(curId))
				result.add(curId);
		}

		return result;
	}

	private List<Integer> userIdsFromWorkflow(List<SbiWhatifWorkflow> workflow) {
		List<Integer> toReturn = new ArrayList<>();
		for (int i = 0; i < workflow.size(); i++) {
			toReturn.add(workflow.get(i).getUserId());
		}

		return toReturn;
	}
	
	public String goNextUserByModel(int modelId){
		logger.debug("IN");
		Session aSession = null;
		Transaction tx = null;
		List<SbiWhatifWorkflow> existing = getWorkflowByModel(modelId);
		

		try {
			aSession = getSession();
			tx = aSession.beginTransaction();

				for(int i=0; i<existing.size(); i++){
				SbiWhatifWorkflow actual = existing.get(i);
				String state = actual.getState();
				if(state.equals(STATE_INPROGRESS)){//if we've found the active user
					//we set value to done
					logger.debug("Actual active user is "+actual.getUserId());
					Query q = aSession.createSQLQuery("UPDATE SBI_WHATIF_WORKFLOW SET state = :st WHERE model_id = :mId AND user_id = :uId");
					q.setParameter("st", STATE_DONE);
					q.setParameter("mId", modelId);
					q.setParameter("uId", actual.getUserId());
					q.executeUpdate();
					logger.debug("Done set state done to actual active user is "+actual.getUserId());
					//if actual is not last user in workflow we enable next user
					if(i<existing.size()-1){
						SbiWhatifWorkflow next = existing.get(i+1);
						logger.debug("Actual active user is "+actual.getUserId());
						Query q1 = aSession.createSQLQuery("UPDATE SBI_WHATIF_WORKFLOW SET state = :st WHERE model_id = :mId AND user_id = :uId");
						q1.setParameter("st", STATE_INPROGRESS);
						q1.setParameter("mId", modelId);
						q1.setParameter("uId", next.getUserId());
						q1.executeUpdate();
						logger.debug("Done set state inprogress to next active user is "+next.getUserId());
						return getUserName(next.getUserId());
						
					}else{
						logger.debug("No other user in the workflow");
					}
					return null;
				}
			}
			tx.commit();
		} catch (Exception he) {
			logger.error("Error setting passing the control to next user", he);
			if (tx != null)
				tx.rollback();
			throw new SpagoBIRuntimeException("Error setting passing the control to next user", he);
		} finally {
			if (aSession != null) {
				if (aSession.isOpen())
					aSession.close();
			}
			logger.debug("Successfully pass controll to next user for model"+modelId);
		}
		return null;

	}
}
