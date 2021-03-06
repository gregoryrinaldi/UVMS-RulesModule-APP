/*
 Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
 and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
 the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.fisheries.uvms.rules.dao;

import eu.europa.ec.fisheries.uvms.commons.service.dao.AbstractDAO;
import eu.europa.ec.fisheries.uvms.rules.entity.FADocumentID;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FADocumentIDDAO extends AbstractDAO<FADocumentID> {

    private EntityManager em;

    public FADocumentIDDAO(EntityManager em) {
        this.em = em;
    }

    @Override public EntityManager getEntityManager() {
        return em;
    }

    public List<FADocumentID> loadFADocumentIDByIdsByIds(Set<FADocumentID> ids) {
        Set<String> stringSet = new HashSet<>();
        for (FADocumentID id : ids) {
            String uuid = id.getUuid();
            if(StringUtils.isNotEmpty(uuid)){
                stringSet.add(uuid);
                stringSet.add(uuid.toLowerCase());
                stringSet.add(uuid.toUpperCase());
            }
        }
        Query query = getEntityManager().createNamedQuery(FADocumentID.LOAD_BY_UUID);
        query.setParameter("uuids", stringSet);
        return query.getResultList();
    }

}
