package com.sky.mapper;

import com.sky.entity.SetMealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetMealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量保存套餐和菜品的关联关系
     * @param setmealDishes
     */
    void insertBatch(List<SetMealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     * @param setMealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setMealId}")
    void deleteBySetMealId(Long setMealId);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     * @param setMealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setMealId}")
    List<SetMealDish> getBySetMealId(Long setMealId);
}
